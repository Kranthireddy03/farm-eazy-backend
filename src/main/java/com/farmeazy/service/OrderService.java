package com.farmeazy.service;

import com.farmeazy.dto.AddressDto;
import com.farmeazy.dto.OrderCreateDto;
import com.farmeazy.dto.OrderDto;
import com.farmeazy.dto.OrderItemDetailDto;
import com.farmeazy.entity.*;
import com.farmeazy.entity.Notification.NotificationPriority;
import com.farmeazy.entity.Notification.NotificationType;
import com.farmeazy.entity.Order.OrderStatus;
import com.farmeazy.entity.Order.PaymentMethod;
import com.farmeazy.entity.Order.PaymentStatus;
import com.farmeazy.entity.UserActivity.ActivityType;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.AddressRepository;
import com.farmeazy.repository.OrderRepository;
import com.farmeazy.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {
    /**
     * Retry payment for failed Razorpay orders
     */
    public OrderDto retryPayment(User user, Long orderId, com.farmeazy.dto.RetryPaymentDto retryDto) {
        // Simulate payment verification (should call Razorpay API in real impl)
        boolean paymentSuccess = verifyRazorpayPayment(retryDto.getPaymentId());

        // Send email for retry result
        if (paymentSuccess) {
            httpEmailService.sendNotificationEmail(
                user.getEmail(),
                user.getUsername(),
                "Payment Successful - FarmEazy",
                "Your payment retry was successful and your order is now confirmed."
            );
            
            // Send SMS for payment success
            try {
                if (user.getPhone() != null && !user.getPhone().isBlank()) {
                    smsService.sendPaymentSuccess(
                        user.getPhone(),
                        user.getUsername(),
                        retryDto.getAmount() != null ? retryDto.getAmount() : "N/A",
                        "ORD" + orderId
                    );
                }
            } catch (Exception smsEx) {
                log.warn("Failed to send payment success SMS for order {}: {}", orderId, smsEx.getMessage());
            }
        } else {
            httpEmailService.sendNotificationEmail(
                user.getEmail(),
                user.getUsername(),
                "Payment Failed - FarmEazy",
                "Your payment retry failed. Please try again or contact support."
            );
            
            // Send SMS for payment failure
            try {
                if (user.getPhone() != null && !user.getPhone().isBlank()) {
                    smsService.sendPaymentFailed(
                        user.getPhone(),
                        user.getUsername(),
                        "ORD" + orderId
                    );
                }
            } catch (Exception smsEx) {
                log.warn("Failed to send payment failed SMS for order {}: {}", orderId, smsEx.getMessage());
            }
        }
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!OrderStatus.PENDING.equals(order.getOrderStatus()) || !PaymentMethod.RAZORPAY.equals(order.getPaymentMethod())) {
            throw new IllegalArgumentException("Retry only allowed for pending Razorpay orders");
        }

        log.info("Razorpay retry payment response: orderId={}, paymentId={}, success={}", orderId, retryDto.getPaymentId(), paymentSuccess);

        if (paymentSuccess) {
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order.setTransactionId(retryDto.getPaymentId());
            order.setPaidAt(LocalDateTime.now());
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setOrderStatus(OrderStatus.PENDING);
        }
        orderRepository.save(order);
        // Log activity
        userActivityService.logActivity(
            user,
            paymentSuccess ? ActivityType.PAYMENT_SUCCESS : ActivityType.PAYMENT_FAILED,
            paymentSuccess ? "Payment retry successful for order #" + orderId : "Payment retry failed for order #" + orderId,
            null,
            String.valueOf(orderId),
            "Order"
        );
        return convertToDto(order);
    }

    // Dummy payment verification (replace with real Razorpay API call)
    private boolean verifyRazorpayPayment(String paymentId) {
        // Accept any non-empty paymentId as success for demo
        return paymentId != null && !paymentId.trim().isEmpty();
    }

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private HttpEmailService httpEmailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DeliveryLocationService deliveryLocationService;

    /**
     * Create new order from cart
     */
    @Transactional
    @CacheEvict(cacheNames = {"orderListUser"}, allEntries = true)
    public OrderDto createOrder(User user, OrderCreateDto createDto) {
        try {
            // Validate order data
            if (createDto.getItems() == null || createDto.getItems().isEmpty()) {
                throw new IllegalArgumentException("Order must have at least one item");
            }

            // Resolve coins used with null safety
            long coinsUsed = createDto.getCoinsUsed() != null ? createDto.getCoinsUsed() : 0L;

            // Resolve shipping address (required for all payment methods)
            Address shippingAddress = null;
            if (createDto.getAddressId() != null) {
                shippingAddress = addressRepository.findByIdAndUser(createDto.getAddressId(), user)
                        .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
            } else if (createDto.getNewAddress() != null) {
                AddressDto savedAddress = addressService.createAddress(user, createDto.getNewAddress());
                shippingAddress = addressRepository.findByIdAndUser(savedAddress.getId(), user)
                        .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
            } else {
                throw new IllegalArgumentException("Shipping address is required");
            }

            // Create order
            Order order = new Order();
            order.setUser(user);
            order.setSubtotal(createDto.getSubtotal());
            order.setTaxAmount(createDto.getTaxAmount());
            order.setTotalAmount(createDto.getTotalAmount());
            order.setCoinsUsed(coinsUsed);
            order.setFinalAmount(createDto.getFinalAmount());
            order.setPaymentMethod(PaymentMethod.valueOf(createDto.getPaymentMethod()));
            order.setShippingAddress(shippingAddress);

            // Set payment and order status based on payment method
            if ("CASH_ON_DELIVERY".equals(createDto.getPaymentMethod())) {
                order.setPaymentStatus(PaymentStatus.PENDING);
                order.setOrderStatus(OrderStatus.CONFIRMED);

                // Add shipping address for COD
            } else if ("RAZORPAY".equals(createDto.getPaymentMethod()) && 
                       createDto.getPaymentId() != null && !createDto.getPaymentId().isBlank()) {
                // Razorpay payment already verified - mark as completed
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setOrderStatus(OrderStatus.CONFIRMED);
                order.setTransactionId(createDto.getPaymentId());
                order.setPaidAt(LocalDateTime.now());
            } else if ("RAZORPAY".equals(createDto.getPaymentMethod())) {
                throw new IllegalArgumentException("Razorpay payment is not successful. Please complete payment before placing order.");
            } else {
                // For other payment methods or Razorpay without paymentId, mark as processing
                order.setPaymentStatus(PaymentStatus.PROCESSING);
                order.setOrderStatus(OrderStatus.PENDING);
            }

            // Create order items
            List<OrderItem> items = new ArrayList<>();
            for (var itemDto : createDto.getItems()) {
                Product product = productRepository.findById(itemDto.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemDto.getProductId()));

                if (!deliveryLocationService.isProductDeliverable(product, null, shippingAddress)) {
                    throw new IllegalArgumentException("Product '" + product.getProductName() + "' is not deliverable to the selected address");
                }

                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setProduct(product);
                item.setQuantity(itemDto.getQuantity());
                item.setPricePerUnit(itemDto.getPrice());
                item.calculateTotalPrice();

                items.add(item);
                   // Deduct product quantity
                   int orderedQty = itemDto.getQuantity();
                   if (product.getQuantity() < orderedQty) {
                       throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
                   }
                   product.setQuantity(product.getQuantity() - orderedQty);
                   productRepository.save(product);
            }
            order.setItems(items);

            // Save order
            Order savedOrder = orderRepository.save(order);

            try {
                notificationService.createForUser(
                    user,
                    NotificationType.ORDER,
                    "Order placed successfully",
                    "Your order #" + savedOrder.getId() + " has been placed.",
                    "/orders",
                    NotificationPriority.NORMAL
                );
            } catch (Exception ignored) {
            }

            try {
                java.util.HashSet<Long> notifiedSellerIds = new java.util.HashSet<>();
                for (OrderItem item : items) {
                    Product itemProduct = item.getProduct();
                    if (itemProduct == null || itemProduct.getSeller() == null || itemProduct.getSeller().getId() == null) {
                        continue;
                    }
                    User seller = itemProduct.getSeller();
                    if (!notifiedSellerIds.add(seller.getId())) {
                        continue;
                    }
                    notificationService.createForUser(
                        seller,
                        NotificationType.ORDER,
                        "You received a new order",
                        "A buyer placed an order containing your product listing(s).",
                        "/orders",
                        NotificationPriority.HIGH
                    );
                }
            } catch (Exception ignored) {
            }

            // Deduct coins if used
            if (coinsUsed > 0) {
                coinService.deductCoins(user, coinsUsed, "Used in order #" + savedOrder.getId());
                
                // Log activity
                userActivityService.logActivity(
                        user,
                        ActivityType.COINS_USED,
                        "Used " + createDto.getCoinsUsed() + " coins in order",
                        "{\"coins\": " + createDto.getCoinsUsed() + ", \"amount\": \"₹" + createDto.getCoinsUsed() + "\"}",
                        String.valueOf(savedOrder.getId()),
                        "Order"
                );
            }

            // Log activity
            userActivityService.logActivity(
                    user,
                    ActivityType.ORDER_PLACED,
                    "Order placed for ₹" + createDto.getFinalAmount(),
                    "{\"items\": " + createDto.getItems().size() + ", \"total\": \"" + createDto.getFinalAmount() + "\"}",
                    String.valueOf(savedOrder.getId()),
                    "Order"
            );

            // Send order communication based on payment method/status.
            // Calculate coin discount: coinsUsed * 1 rupee per coin
            BigDecimal coinDiscount = BigDecimal.valueOf(savedOrder.getCoinsUsed());

            PaymentMethod paymentMethod = savedOrder.getPaymentMethod();
            PaymentStatus paymentStatus = savedOrder.getPaymentStatus();
            OrderStatus orderStatus = savedOrder.getOrderStatus();
            boolean isCodOrder = PaymentMethod.CASH_ON_DELIVERY.equals(paymentMethod);
            boolean isPaymentCompleted = PaymentStatus.COMPLETED.equals(paymentStatus);
            boolean isPaymentFailed = PaymentStatus.FAILED.equals(paymentStatus);

            if (isPaymentCompleted || (isCodOrder && PaymentStatus.PENDING.equals(paymentStatus))) {
                String orderDate = savedOrder.getCreatedAt() == null
                        ? null
                        : savedOrder.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
                String orderItemsHtml = buildOrderItemsHtml(savedOrder.getItems());
                String deliveryAddress = buildDeliveryAddress(savedOrder.getShippingAddress());
                String trackOrderUrl = "/orders";

                httpEmailService.sendOrderConfirmationEmailAsync(
                    user.getEmail(),
                    user.getUsername(),
                    savedOrder.getId(),
                    savedOrder.getSubtotal().toPlainString(),
                    coinDiscount.toPlainString(),
                    savedOrder.getTaxAmount().toPlainString(),
                    savedOrder.getFinalAmount().toPlainString(),
                    paymentMethod.name(),
                    paymentStatus.name(),
                    orderStatus.name(),
                    orderDate,
                    orderItemsHtml,
                    deliveryAddress,
                    trackOrderUrl
                );

                // Send SMS notification based on order type.
                try {
                    if (user.getPhone() != null && !user.getPhone().isBlank()) {
                        if (isPaymentCompleted) {
                            smsService.sendPaymentSuccess(
                                user.getPhone(),
                                user.getUsername(),
                                savedOrder.getFinalAmount().toPlainString(),
                                "ORD" + savedOrder.getId()
                            );
                        }
                        smsService.sendBookingConfirm(
                            user.getPhone(),
                            user.getUsername(),
                            "ORD" + savedOrder.getId()
                        );
                    }
                } catch (Exception smsEx) {
                    log.warn("Failed to send order confirmation SMS for order {}: {}", savedOrder.getId(), smsEx.getMessage());
                }
            } else if (isPaymentFailed) {
                // Optional: Send payment failure email
                httpEmailService.sendNotificationEmail(
                        user.getEmail(),
                        user.getUsername(),
                        "Payment Failed - FarmEazy",
                        "Your payment for order #" + savedOrder.getId() + " was not successful. Please retry payment or contact support if you need help."
                );
                
                // Send SMS for payment failure
                try {
                    if (user.getPhone() != null && !user.getPhone().isBlank()) {
                        smsService.sendPaymentFailed(
                            user.getPhone(),
                            user.getUsername(),
                            "ORD" + savedOrder.getId()
                        );
                    }
                } catch (Exception smsEx) {
                    log.warn("Failed to send payment failed SMS for order {}: {}", savedOrder.getId(), smsEx.getMessage());
                }
            }

            log.info("Order created successfully: {}", savedOrder.getId());
            return convertToDto(savedOrder);

        } catch (Exception e) {
            log.error("Error creating order for user {}: {}", user.getId(), e.getMessage(), e);
            userActivityService.logActivity(
                    user,
                    ActivityType.PAYMENT_FAILED,
                    "Order creation failed: " + e.getMessage(),
                    null,
                    null,
                    null
            );
            throw e;
        }
    }

    @Cacheable(cacheNames = "orderListUser", key = "#user.id", unless = "#result == null || #result.isEmpty()")
    public List<OrderDto> getUserOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get specific order
     */
    @Cacheable(cacheNames = "orderById", key = "#orderId + ':' + #user.id", unless = "#result == null")
    public OrderDto getOrder(User user, Long orderId) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return convertToDto(order);
    }

    /**
     * Get user's orders
     */
    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        try {
            OrderStatus newStatus = OrderStatus.valueOf(status);
            order.setOrderStatus(newStatus);

            if (OrderStatus.DELIVERED.equals(newStatus)) {
                order.setUpdatedAt(LocalDateTime.now());
                
                // Log activity
                userActivityService.logActivity(
                        order.getUser(),
                        ActivityType.ORDER_DELIVERED,
                        "Order #" + orderId + " has been delivered",
                        null,
                        String.valueOf(orderId),
                        "Order"
                );
            }

            orderRepository.save(order);
            log.info("Order status updated: {} -> {}", orderId, status);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
    }

    /**
     * Confirm payment for order
     */
    @Transactional
    public void confirmPayment(Long orderId, String transactionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.CONFIRMED);

        orderRepository.save(order);

        // Log activity
        userActivityService.logActivity(
                order.getUser(),
                ActivityType.ORDER_PAID,
                "Payment received for order #" + orderId,
                "{\"transactionId\": \"" + transactionId + "\"}",
                String.valueOf(orderId),
                "Order"
        );

        log.info("Payment confirmed for order: {}", orderId);
    }

    /**
     * Cancel order
     */
    @Transactional
    public void cancelOrder(Long orderId, User user) {
                // Send email for cancellation
                httpEmailService.sendNotificationEmail(
                    user.getEmail(),
                    user.getUsername(),
                    "Order Cancelled - FarmEazy",
                    "Your order has been cancelled. If you have questions, please contact support."
                );
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (OrderStatus.DELIVERED.equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Cannot cancel delivered order");
        }

        // Refund coins if used
        if (order.getCoinsUsed() > 0) {
            coinService.addCoins(user, order.getCoinsUsed(), "Refund for cancelled order #" + orderId);
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);
        orderRepository.save(order);

        // Restore product quantity for cancelled order
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);
        }
        // Log activity
        userActivityService.logActivity(
                user,
                ActivityType.ORDER_CANCELLED,
                "Order #" + orderId + " has been cancelled",
                null,
                String.valueOf(orderId),
                "Order"
        );

        log.info("Order cancelled: {}", orderId);

                    // Booking cancelled SMS
                    try {
                        if (user.getPhone() != null && !user.getPhone().isBlank()) {
                            smsService.sendBookingCancelled(
                                user.getPhone(),
                                user.getUsername(),
                                "ORD" + orderId
                            );
                        }
                    } catch (Exception smsEx) {
                        log.warn("Failed to send booking cancelled SMS for order {}: {}", orderId, smsEx.getMessage());
                    }
    }

    /**
     * Mark order as refund initiated
     * 
     * WHY: After user provides refund details, we initiate the refund process.
     * This updates the order status to track refund progress.
     */
    @Transactional
    public void markRefundInitiated(Long orderId, User user) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!OrderStatus.CANCELLED.equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Only cancelled orders can have refund initiated");
        }

        order.setPaymentStatus(PaymentStatus.REFUND_INITIATED);
        orderRepository.save(order);

        // Log activity
        userActivityService.logActivity(
                user,
                ActivityType.REFUND_INITIATED,
                "Refund initiated for Order #" + orderId,
                null,
                String.valueOf(orderId),
                "Order"
        );

        log.info("Refund initiated for order: {}", orderId);
    }

    /**
     * Get order count for user
     */
    public Long getUserOrderCount(User user) {
        return orderRepository.countByUser(user);
    }

    private String buildOrderItemsHtml(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "<tr><td colspan=\"4\" style=\"padding:10px; border:1px solid #e2e8f0; text-align:center; color:#6b7280;\">No items found for this order.</td></tr>";
        }

        StringBuilder rows = new StringBuilder();
        for (OrderItem item : items) {
            if (item == null) {
                continue;
            }

            String productName = "Product";
            if (item.getProduct() != null) {
                String name = item.getProduct().getProductName();
                if (name == null || name.isBlank()) {
                    name = item.getProduct().getName();
                }
                if (name != null && !name.isBlank()) {
                    productName = name;
                }
            }

            BigDecimal unitPrice = item.getPricePerUnit() == null ? BigDecimal.ZERO : item.getPricePerUnit();
            BigDecimal lineTotal = item.getTotalPrice() == null
                    ? unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
                    : item.getTotalPrice();

            rows.append("<tr>")
                    .append("<td style=\"padding:10px; border:1px solid #e2e8f0; word-break:break-word;\">")
                    .append(escapeHtml(productName))
                    .append("</td>")
                    .append("<td align=\"center\" style=\"padding:10px; border:1px solid #e2e8f0;\">")
                    .append(item.getQuantity())
                    .append("</td>")
                    .append("<td align=\"right\" style=\"padding:10px; border:1px solid #e2e8f0;\">₹")
                    .append(formatAmount(unitPrice))
                    .append("</td>")
                    .append("<td align=\"right\" style=\"padding:10px; border:1px solid #e2e8f0;\">₹")
                    .append(formatAmount(lineTotal))
                    .append("</td>")
                    .append("</tr>");
        }

        if (rows.length() == 0) {
            return "<tr><td colspan=\"4\" style=\"padding:10px; border:1px solid #e2e8f0; text-align:center; color:#6b7280;\">No items found for this order.</td></tr>";
        }
        return rows.toString();
    }

    private String buildDeliveryAddress(Address address) {
        if (address == null) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        if (address.getFullName() != null && !address.getFullName().isBlank()) {
            lines.add(escapeHtml(address.getFullName()));
        }
        if (address.getPhoneNumber() != null && !address.getPhoneNumber().isBlank()) {
            lines.add(escapeHtml(address.getPhoneNumber()));
        }
        if (address.getAddressLine1() != null && !address.getAddressLine1().isBlank()) {
            lines.add(escapeHtml(address.getAddressLine1()));
        }
        if (address.getAddressLine2() != null && !address.getAddressLine2().isBlank()) {
            lines.add(escapeHtml(address.getAddressLine2()));
        }

        StringBuilder cityStatePostal = new StringBuilder();
        if (address.getCity() != null && !address.getCity().isBlank()) {
            cityStatePostal.append(address.getCity().trim());
        }
        if (address.getState() != null && !address.getState().isBlank()) {
            if (!cityStatePostal.isEmpty()) {
                cityStatePostal.append(", ");
            }
            cityStatePostal.append(address.getState().trim());
        }
        if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
            if (!cityStatePostal.isEmpty()) {
                cityStatePostal.append(" - ");
            }
            cityStatePostal.append(address.getPostalCode().trim());
        }
        if (!cityStatePostal.isEmpty()) {
            lines.add(escapeHtml(cityStatePostal.toString()));
        }

        if (address.getCountry() != null && !address.getCountry().isBlank()) {
            lines.add(escapeHtml(address.getCountry()));
        }

        return String.join("<br>", lines);
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return safeAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Convert Order entity to DTO
     */
    private OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setSubtotal(order.getSubtotal());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCoinsUsed(order.getCoinsUsed());
        dto.setFinalAmount(order.getFinalAmount());
        dto.setOrderStatus(order.getOrderStatus().name());
        dto.setPaymentStatus(order.getPaymentStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod().name());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        // Refund fields
        if (order.getRefundStatus() != null) {
            dto.setRefundStatus(order.getRefundStatus().name());
        }
        dto.setRefundAmount(order.getRefundAmount());
        dto.setCoinsRefunded(order.getCoinsRefunded());
        dto.setRefundReason(order.getRefundReason());
        if (order.getRefundType() != null) {
            dto.setRefundType(order.getRefundType().name());
        }
        dto.setRefundRequestedAt(order.getRefundRequestedAt());
        dto.setRefundCompletedAt(order.getRefundCompletedAt());
        dto.setCanCancel(order.canBeCancelled());
        dto.setCanReturn(order.canBeReturned());

        if (order.getShippingAddress() != null) {
            dto.setShippingAddress(addressService.convertToDto(order.getShippingAddress()));
        }

        List<OrderItemDetailDto> itemDtos = order.getItems().stream()
                .map(this::convertOrderItemToDto)
                .collect(Collectors.toList());
        dto.setItems(itemDtos);

        return dto;
    }

    private OrderItemDetailDto convertOrderItemToDto(OrderItem item) {
        OrderItemDetailDto dto = new OrderItemDetailDto();
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPricePerUnit());
        dto.setTotalPrice(item.getTotalPrice());
        // Add media URLs for gallery
        if (item.getProduct().getMediaFiles() != null) {
            java.util.List<String> mediaUrls = item.getProduct().getMediaFiles().stream()
                .map(com.farmeazy.entity.ProductMedia::getMediaUrl)
                .filter(url -> url != null && !url.isEmpty())
                .collect(java.util.stream.Collectors.toList());
            dto.setMediaUrls(mediaUrls);
        }
        return dto;
    }
}

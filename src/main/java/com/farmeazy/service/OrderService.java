package com.farmeazy.service;

import com.farmeazy.dto.AddressDto;
import com.farmeazy.dto.OrderCreateDto;
import com.farmeazy.dto.OrderDto;
import com.farmeazy.dto.OrderItemDetailDto;
import com.farmeazy.entity.*;
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
        } else {
            httpEmailService.sendNotificationEmail(
                user.getEmail(),
                user.getUsername(),
                "Payment Failed - FarmEazy",
                "Your payment retry failed. Please try again or contact support."
            );
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

    /**
     * Create new order from cart
     */
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
            } else {
                // For UPI/PhonePay, mark as processing
                order.setPaymentStatus(PaymentStatus.PROCESSING);
                order.setOrderStatus(OrderStatus.PENDING);
            }

            // Create order items
            List<OrderItem> items = new ArrayList<>();
            for (var itemDto : createDto.getItems()) {
                Product product = productRepository.findById(itemDto.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemDto.getProductId()));

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

            // Send confirmation email using HTTP service (works on Render)
            // Calculate coin discount: coinsUsed * 1 rupee per coin
            BigDecimal coinDiscount = BigDecimal.valueOf(savedOrder.getCoinsUsed());

            // Only send confirmation email if payment is already COMPLETED (e.g., COD or payment completed)
            if (savedOrder.getPaymentStatus() == PaymentStatus.COMPLETED || savedOrder.getPaymentStatus() == PaymentStatus.PENDING) {
                // For COD, PENDING is considered as placed
                httpEmailService.sendOrderConfirmationEmailAsync(
                    user.getEmail(),
                    user.getUsername(),
                    savedOrder.getId(),
                    savedOrder.getSubtotal().toPlainString(),
                    coinDiscount.toPlainString(),
                    savedOrder.getTaxAmount().toPlainString(),
                    savedOrder.getFinalAmount().toPlainString()
                );
            } else if (savedOrder.getPaymentStatus() == PaymentStatus.FAILED) {
                // Optional: Send payment failure email
                httpEmailService.sendNotificationEmail(
                        user.getEmail(),
                        user.getUsername(),
                        "Payment Failed - FarmEazy",
                        "Your payment for order #" + savedOrder.getId() + " was not successful. Please retry payment or contact support if you need help."
                );
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

    /**
     * Get user's orders
     */
    public List<OrderDto> getUserOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get specific order
     */
    public OrderDto getOrder(User user, Long orderId) {
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return convertToDto(order);
    }

    /**
     * Update order status (for admin/seller)
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
    }

    /**
     * Get order count for user
     */
    public Long getUserOrderCount(User user) {
        return orderRepository.countByUser(user);
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
        return dto;
    }
}

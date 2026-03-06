package com.farmeazy.service;

import com.farmeazy.dto.CancelOrderRequestDto;
import com.farmeazy.dto.CancelOrderResponseDto;
import com.farmeazy.entity.Order;
import com.farmeazy.entity.Order.OrderStatus;
import com.farmeazy.entity.Order.PaymentStatus;
import com.farmeazy.entity.Order.RefundStatus;
import com.farmeazy.entity.Order.RefundType;
import com.farmeazy.entity.RefundAuditLog;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.OrderRepository;
import com.farmeazy.repository.RefundAuditLogRepository;
import com.farmeazy.repository.UserRefundDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for handling order cancellations and return requests.
 * 
 * @author FarmEazy Development Team
 */
@Service
@Transactional
public class OrderCancellationService {

    private static final Logger log = LoggerFactory.getLogger(OrderCancellationService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRefundDetailsRepository refundDetailsRepository;

    @Autowired
    private RefundAuditLogRepository auditLogRepository;

    @Autowired
    private UserRefundDetailsService refundDetailsService;

    @Autowired
    private HttpEmailService emailService;

    /**
     * Cancel an order and initiate refund process.
     * Returns REFUND_DETAILS_REQUIRED if user hasn't added bank/UPI details.
     */
    public CancelOrderResponseDto cancelOrder(User user, CancelOrderRequestDto request) {
        log.info("Cancel order request from user: {} for order: {}", user.getEmail(), request.getOrderId());

        // Find the order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Verify ownership
        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to cancel this order");
        }

        // Check if order can be cancelled
        if (!order.canBeCancelled()) {
            if (order.getOrderStatus() == OrderStatus.SHIPPED) {
                return CancelOrderResponseDto.error("Order has already been shipped. Please request a return after delivery.");
            }
            if (order.getOrderStatus() == OrderStatus.DELIVERED) {
                return CancelOrderResponseDto.error("Order has been delivered. Please use the return option instead.");
            }
            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                return CancelOrderResponseDto.error("Order is already cancelled.");
            }
            if (order.getRefundStatus() != RefundStatus.NOT_REQUESTED) {
                return CancelOrderResponseDto.error("A refund has already been requested for this order.");
            }
            if (order.getCancellationDeadline() != null && LocalDateTime.now().isAfter(order.getCancellationDeadline())) {
                return CancelOrderResponseDto.error("Cancellation window has expired.");
            }
            return CancelOrderResponseDto.error("This order cannot be cancelled.");
        }

        // Check if payment was completed (needs refund)
        boolean needsRefund = order.getPaymentStatus() == PaymentStatus.COMPLETED;

        if (needsRefund) {
            // Check if user has refund details
            boolean hasRefundDetails = refundDetailsService.hasValidRefundDetails(user);

            if (!hasRefundDetails) {
                // User needs to add refund details first
                order.setRefundStatus(RefundStatus.REFUND_DETAILS_REQUIRED);
                order.setRefundReason(request.getReason());
                order.setRefundType(RefundType.CANCELLATION);
                order.setRefundRequestedAt(LocalDateTime.now());
                orderRepository.save(order);

                log.info("Refund details required for order: {}", order.getId());
                return CancelOrderResponseDto.refundDetailsRequired(order.getId());
            }

            // Has refund details, proceed with refund request
            return processRefundRequest(order, user, request.getReason(), RefundType.CANCELLATION);
        } else {
            // No payment made, just cancel
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setRefundStatus(RefundStatus.NOT_REQUESTED);
            order.setRefundReason(request.getReason());
            orderRepository.save(order);

            // Create audit log
            RefundAuditLog auditLog = new RefundAuditLog(order, user, "ORDER_CANCELLED_NO_PAYMENT");
            auditLog.setNotes(request.getReason());
            auditLogRepository.save(auditLog);

            log.info("Order cancelled (no payment): {}", order.getId());

            CancelOrderResponseDto response = new CancelOrderResponseDto();
            response.setOrderId(order.getId());
            response.setStatus("CANCELLED");
            response.setMessage("Order has been cancelled successfully.");
            response.setRefundDetailsRequired(false);
            return response;
        }
    }

    /**
     * Request return for a delivered order.
     */
    public CancelOrderResponseDto requestReturn(User user, CancelOrderRequestDto request) {
        log.info("Return request from user: {} for order: {}", user.getEmail(), request.getOrderId());

        // Find the order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Verify ownership
        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to return this order");
        }

        // Check if order can be returned
        if (!order.canBeReturned()) {
            if (order.getOrderStatus() != OrderStatus.DELIVERED) {
                return CancelOrderResponseDto.error("Order must be delivered before requesting a return.");
            }
            if (order.getRefundStatus() != RefundStatus.NOT_REQUESTED) {
                return CancelOrderResponseDto.error("A refund has already been requested for this order.");
            }
            if (order.getReturnDeadline() != null && LocalDateTime.now().isAfter(order.getReturnDeadline())) {
                return CancelOrderResponseDto.error("Return window has expired.");
            }
            return CancelOrderResponseDto.error("This order cannot be returned.");
        }

        // Check if user has refund details
        boolean hasRefundDetails = refundDetailsService.hasValidRefundDetails(user);

        if (!hasRefundDetails) {
            order.setRefundStatus(RefundStatus.REFUND_DETAILS_REQUIRED);
            order.setRefundReason(request.getReason());
            order.setRefundType(RefundType.RETURN);
            order.setRefundRequestedAt(LocalDateTime.now());
            orderRepository.save(order);

            log.info("Refund details required for return: {}", order.getId());
            return CancelOrderResponseDto.refundDetailsRequired(order.getId());
        }

        // Has refund details, proceed with return request
        return processRefundRequest(order, user, request.getReason(), RefundType.RETURN);
    }

    /**
     * Process refund request after user has added refund details.
     * Called after user adds bank/UPI details when REFUND_DETAILS_REQUIRED.
     */
    public CancelOrderResponseDto proceedWithRefund(User user, Long orderId) {
        log.info("Proceeding with refund for order: {} after details added", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Verify ownership
        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to access this order");
        }

        // Check if order is waiting for refund details
        if (order.getRefundStatus() != RefundStatus.REFUND_DETAILS_REQUIRED) {
            return CancelOrderResponseDto.error("This order is not waiting for refund details.");
        }

        // Verify user now has refund details
        if (!refundDetailsService.hasValidRefundDetails(user)) {
            return CancelOrderResponseDto.refundDetailsRequired(orderId);
        }

        // Process the refund
        return processRefundRequest(order, user, order.getRefundReason(), order.getRefundType());
    }

    /**
     * Internal method to process refund request.
     */
    private CancelOrderResponseDto processRefundRequest(Order order, User user, String reason, RefundType refundType) {
        // Calculate refund amount (money paid after coins discount)
        order.calculateRefundAmount();

        // Update order status
        order.setRefundStatus(RefundStatus.REQUESTED);
        order.setRefundReason(reason);
        order.setRefundType(refundType);
        order.setRefundRequestedAt(LocalDateTime.now());

        // Cancel the order if it's a cancellation
        if (refundType == RefundType.CANCELLATION) {
            order.setOrderStatus(OrderStatus.CANCELLED);
        }

        orderRepository.save(order);

        // Create audit log
        RefundAuditLog auditLog = RefundAuditLog.createRequestLog(order, user, reason);
        auditLog.setRazorpayPaymentId(order.getRazorpayPaymentId());
        auditLogRepository.save(auditLog);

        // Send professional email notification
        try {
            Long coinsUsed = order.getCoinsUsed() != null ? order.getCoinsUsed() : 0L;
            
            if (refundType == RefundType.CANCELLATION) {
                // Determine if refund details are required
                boolean refundDetailsRequired = order.getRefundStatus() == RefundStatus.REFUND_DETAILS_REQUIRED;
                
                emailService.sendOrderCancellationNotification(
                        user.getEmail(),
                        user.getUsername(),
                        order.getId(),
                        reason,
                        order.getRefundAmount(),
                        coinsUsed,
                        refundDetailsRequired
                );
            } else {
                // Return request notification
                emailService.sendReturnRequestNotification(
                        user.getEmail(),
                        user.getUsername(),
                        order.getId(),
                        reason,
                        order.getRefundAmount(),
                        coinsUsed
                );
            }
        } catch (Exception e) {
            log.error("Failed to send refund notification email", e);
        }

        log.info("Refund request processed for order: {}, amount: {}, coinsUsed: {}", 
                order.getId(), order.getRefundAmount(), order.getCoinsUsed());

        Long coinsToRefund = order.getCoinsUsed() != null ? order.getCoinsUsed() : 0L;
        return CancelOrderResponseDto.refundInitiated(order.getId(), order.getRefundAmount(), coinsToRefund);
    }

    /**
     * Get refund status for an order.
     */
    public CancelOrderResponseDto getRefundStatus(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to view this order");
        }

        CancelOrderResponseDto response = new CancelOrderResponseDto();
        response.setOrderId(orderId);
        response.setRefundStatus(order.getRefundStatus() != null ? order.getRefundStatus().name() : null);
        response.setRefundAmount(order.getRefundAmount());
        response.setRefundDetailsRequired(order.getRefundStatus() == RefundStatus.REFUND_DETAILS_REQUIRED);

        switch (order.getRefundStatus()) {
            case NOT_REQUESTED:
                response.setStatus("NO_REFUND");
                response.setMessage("No refund has been requested for this order.");
                break;
            case REFUND_DETAILS_REQUIRED:
                response.setStatus("REFUND_DETAILS_REQUIRED");
                response.setMessage("Please add your bank/UPI details to receive the refund.");
                break;
            case REQUESTED:
                response.setStatus("PENDING");
                response.setMessage("Your refund request is being processed.");
                break;
            case APPROVED:
            case PROCESSING:
                response.setStatus("PROCESSING");
                response.setMessage("Your refund is being processed. It will be credited within 3-5 business days.");
                break;
            case COMPLETED:
                response.setStatus("COMPLETED");
                response.setMessage("Your refund has been credited successfully.");
                break;
            case FAILED:
                response.setStatus("FAILED");
                response.setMessage("Refund processing failed. We will retry automatically.");
                break;
            case REJECTED:
                response.setStatus("REJECTED");
                response.setMessage("Your refund request was not approved. " + 
                        (order.getRefundAdminNotes() != null ? order.getRefundAdminNotes() : ""));
                break;
            default:
                response.setStatus(order.getRefundStatus().name());
                response.setMessage("Refund status: " + order.getRefundStatus().name());
        }

        return response;
    }
}

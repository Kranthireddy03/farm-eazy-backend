package com.farmeazy.controller;

import com.farmeazy.dto.CancelOrderRequestDto;
import com.farmeazy.dto.CancelOrderResponseDto;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.OrderCancellationService;
import com.farmeazy.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for order cancellation, returns, and refund management.
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
@Tag(name = "Order Cancellation & Refunds", description = "Cancel orders, request returns, and track refunds")
public class OrderCancellationController {

    private static final Logger logger = LoggerFactory.getLogger(OrderCancellationController.class);

    @Autowired
    private OrderCancellationService cancellationService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Cancel an order.
     * Returns REFUND_DETAILS_REQUIRED if user hasn't added bank/UPI details.
     */
    @PostMapping("/cancel")
    @Operation(summary = "Cancel order", description = "Cancel an order and initiate refund if payment was made")
    public ResponseEntity<CancelOrderResponseDto> cancelOrder(
            Authentication authentication,
            @Valid @RequestBody CancelOrderRequestDto request) {
        logger.info("ORDER_CANCELLATION_CONTROLLER_CANCEL user={} orderId={}", authentication != null ? authentication.getName() : null, request != null ? request.getOrderId() : null);
        User user = getCurrentUser(authentication);
        CancelOrderResponseDto response = cancellationService.cancelOrder(user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Request return for a delivered order.
     */
    @PostMapping("/return")
    @Operation(summary = "Request return", description = "Request return for a delivered order")
    public ResponseEntity<CancelOrderResponseDto> requestReturn(
            Authentication authentication,
            @Valid @RequestBody CancelOrderRequestDto request) {
        logger.info("ORDER_CANCELLATION_CONTROLLER_RETURN user={} orderId={}", authentication != null ? authentication.getName() : null, request != null ? request.getOrderId() : null);
        User user = getCurrentUser(authentication);
        CancelOrderResponseDto response = cancellationService.requestReturn(user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Proceed with refund after user adds refund details.
     * Called when order is in REFUND_DETAILS_REQUIRED status.
     */
    @PostMapping("/{orderId}/proceed-refund")
    @Operation(summary = "Proceed with refund", description = "Continue refund process after adding bank/UPI details")
    public ResponseEntity<CancelOrderResponseDto> proceedWithRefund(
            Authentication authentication,
            @PathVariable Long orderId) {
        logger.info("ORDER_CANCELLATION_CONTROLLER_PROCEED_REFUND user={} orderId={}", authentication != null ? authentication.getName() : null, orderId);
        User user = getCurrentUser(authentication);
        CancelOrderResponseDto response = cancellationService.proceedWithRefund(user, orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get refund status for an order.
     */
    @GetMapping("/{orderId}/refund-status")
    @Operation(summary = "Get refund status", description = "Get detailed refund status for an order")
    public ResponseEntity<CancelOrderResponseDto> getRefundStatus(
            Authentication authentication,
            @PathVariable Long orderId) {
        logger.info("ORDER_CANCELLATION_CONTROLLER_REFUND_STATUS user={} orderId={}", authentication != null ? authentication.getName() : null, orderId);
        User user = getCurrentUser(authentication);
        CancelOrderResponseDto response = cancellationService.getRefundStatus(user, orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if order can be cancelled.
     */
    @GetMapping("/{orderId}/can-cancel")
    @Operation(summary = "Check cancellability", description = "Check if order can be cancelled")
    public ResponseEntity<?> checkCanCancel(
            Authentication authentication,
            @PathVariable Long orderId) {
        logger.info("ORDER_CANCELLATION_CONTROLLER_CAN_CANCEL user={} orderId={}", authentication != null ? authentication.getName() : null, orderId);
        User user = getCurrentUser(authentication);
        CancelOrderResponseDto status = cancellationService.getRefundStatus(user, orderId);
        
        // Check order status from OrderService
        // For now, return based on refund status
        boolean canCancel = status.getRefundStatus() == null || "NOT_REQUESTED".equals(status.getRefundStatus());
        
        return ResponseEntity.ok(Map.of(
            "canCancel", canCancel,
            "reason", canCancel ? "Order can be cancelled" : "Order cannot be cancelled: " + status.getMessage()
        ));
    }

    /**
     * Admin: Approve a refund request.
     */
    @PostMapping("/admin/{orderId}/approve-refund")
    @Operation(summary = "Approve refund (Admin)", description = "Admin action to approve a refund request")
    public ResponseEntity<?> approveRefund(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> body) {
        logger.info("ORDER_CANCELLATION_CONTROLLER_APPROVE_REFUND user={} orderId={}", authentication != null ? authentication.getName() : null, orderId);
        User adminUser = getCurrentUser(authentication);
        // TODO: Add admin role check
        
        String notes = body != null ? body.getOrDefault("notes", "Approved by admin") : "Approved by admin";
        refundService.approveRefund(orderId, adminUser, notes);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Refund approved successfully"
        ));
    }

    /**
     * Admin: Reject a refund request.
     */
    @PostMapping("/admin/{orderId}/reject-refund")
    @Operation(summary = "Reject refund (Admin)", description = "Admin action to reject a refund request")
    public ResponseEntity<?> rejectRefund(
            Authentication authentication,
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body) {
        logger.info("ORDER_CANCELLATION_CONTROLLER_REJECT_REFUND user={} orderId={}", authentication != null ? authentication.getName() : null, orderId);
        User adminUser = getCurrentUser(authentication);
        // TODO: Add admin role check
        
        String reason = body.getOrDefault("reason", "Request does not meet refund policy");
        refundService.rejectRefund(orderId, adminUser, reason);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Refund rejected"
        ));
    }

    /**
     * Admin: Manually trigger refund processing.
     */
    @PostMapping("/admin/{orderId}/process-refund")
    @Operation(summary = "Process refund (Admin)", description = "Admin action to manually trigger refund processing")
    public ResponseEntity<?> processRefund(@PathVariable Long orderId) {
        logger.info("ORDER_CANCELLATION_CONTROLLER_PROCESS_REFUND orderId={}", orderId);
        // TODO: Add admin role check
        
        refundService.triggerManualRefund(orderId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Refund processing triggered"
        ));
    }

    /**
     * Admin: Get refund statistics.
     */
    @GetMapping("/admin/refund-stats")
    @Operation(summary = "Get refund stats (Admin)", description = "Get refund processing statistics")
    public ResponseEntity<?> getRefundStats() {
        logger.info("ORDER_CANCELLATION_CONTROLLER_REFUND_STATS");
        // TODO: Add admin role check
        
        RefundService.RefundStats stats = refundService.getRefundStats();
        
        return ResponseEntity.ok(Map.of(
            "pending", stats.pendingCount,
            "processing", stats.processingCount,
            "completed", stats.completedCount,
            "failed", stats.failedCount,
            "rejected", stats.rejectedCount
        ));
    }

    /**
     * Get current authenticated user.
     */
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

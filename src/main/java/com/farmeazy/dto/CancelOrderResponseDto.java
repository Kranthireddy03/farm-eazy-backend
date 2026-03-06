package com.farmeazy.dto;

import java.math.BigDecimal;

/**
 * Response DTO for cancel/refund operations.
 */
public class CancelOrderResponseDto {

    private Long orderId;
    private String status; // REFUND_INITIATED, REFUND_DETAILS_REQUIRED, CANCELLED, ERROR
    private String message;
    private BigDecimal refundAmount;
    private Long coinsToRefund;
    private String refundStatus;
    private String estimatedRefundDate;
    private boolean refundDetailsRequired;

    // Constructors
    public CancelOrderResponseDto() {
    }

    public CancelOrderResponseDto(String status, String message) {
        this.status = status;
        this.message = message;
    }

    // Static factory methods
    public static CancelOrderResponseDto refundDetailsRequired(Long orderId) {
        CancelOrderResponseDto response = new CancelOrderResponseDto();
        response.setOrderId(orderId);
        response.setStatus("REFUND_DETAILS_REQUIRED");
        response.setMessage("Please add your bank/UPI details to receive the refund");
        response.setRefundDetailsRequired(true);
        return response;
    }

    public static CancelOrderResponseDto refundInitiated(Long orderId, BigDecimal amount) {
        CancelOrderResponseDto response = new CancelOrderResponseDto();
        response.setOrderId(orderId);
        response.setStatus("REFUND_INITIATED");
        response.setMessage("Your refund request has been submitted. Amount will be credited within 5-7 business days.");
        response.setRefundAmount(amount);
        response.setRefundStatus("REQUESTED");
        response.setRefundDetailsRequired(false);
        return response;
    }

    public static CancelOrderResponseDto refundInitiated(Long orderId, BigDecimal amount, Long coinsToRefund) {
        CancelOrderResponseDto response = new CancelOrderResponseDto();
        response.setOrderId(orderId);
        response.setStatus("REFUND_INITIATED");
        
        // Build appropriate message based on what's being refunded
        StringBuilder message = new StringBuilder("Your refund request has been submitted.");
        if (coinsToRefund != null && coinsToRefund > 0 && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            message.append(String.format(" %d coins + ₹%.2f will be refunded.", coinsToRefund, amount));
        } else if (coinsToRefund != null && coinsToRefund > 0) {
            message.append(String.format(" %d coins will be credited to your account.", coinsToRefund));
        } else if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            message.append(String.format(" ₹%.2f will be credited within 5-7 business days.", amount));
        }
        
        response.setMessage(message.toString());
        response.setRefundAmount(amount);
        response.setCoinsToRefund(coinsToRefund);
        response.setRefundStatus("REQUESTED");
        response.setRefundDetailsRequired(false);
        return response;
    }

    public static CancelOrderResponseDto error(String message) {
        CancelOrderResponseDto response = new CancelOrderResponseDto();
        response.setStatus("ERROR");
        response.setMessage(message);
        return response;
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getEstimatedRefundDate() {
        return estimatedRefundDate;
    }

    public void setEstimatedRefundDate(String estimatedRefundDate) {
        this.estimatedRefundDate = estimatedRefundDate;
    }

    public boolean isRefundDetailsRequired() {
        return refundDetailsRequired;
    }

    public void setRefundDetailsRequired(boolean refundDetailsRequired) {
        this.refundDetailsRequired = refundDetailsRequired;
    }

    public Long getCoinsToRefund() {
        return coinsToRefund;
    }

    public void setCoinsToRefund(Long coinsToRefund) {
        this.coinsToRefund = coinsToRefund;
    }
}

package com.farmeazy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for cancel/return order request.
 */
public class CancelOrderRequestDto {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 10, max = 500, message = "Reason must be 10-500 characters")
    private String reason;

    private String refundType = "CANCELLATION"; // CANCELLATION, RETURN, PARTIAL_RETURN

    // Constructors
    public CancelOrderRequestDto() {
    }

    public CancelOrderRequestDto(Long orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRefundType() {
        return refundType;
    }

    public void setRefundType(String refundType) {
        this.refundType = refundType;
    }
}

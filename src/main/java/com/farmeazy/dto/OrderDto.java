package com.farmeazy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {
    private Long id;
    private Long userId;
    private String orderNumber;
    private List<OrderItemDetailDto> items;
    private AddressDto shippingAddress;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private Long coinsUsed;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Refund fields
    private String refundStatus;
    private BigDecimal refundAmount;
    private Long coinsRefunded;
    private String refundReason;
    private String refundType;
    private LocalDateTime refundRequestedAt;
    private LocalDateTime refundCompletedAt;
    private boolean canCancel;
    private boolean canReturn;

    public OrderDto() {
    }

    public OrderDto(Long id, Long userId, String orderNumber, List<OrderItemDetailDto> items, AddressDto shippingAddress, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal totalAmount, Long coinsUsed, BigDecimal finalAmount, String paymentMethod, String paymentStatus, String orderStatus, String transactionId, LocalDateTime paidAt, LocalDateTime estimatedDeliveryDate, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.orderNumber = orderNumber;
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.coinsUsed = coinsUsed;
        this.finalAmount = finalAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.transactionId = transactionId;
        this.paidAt = paidAt;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public List<OrderItemDetailDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDetailDto> items) {
        this.items = items;
    }

    public AddressDto getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(AddressDto shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getCoinsUsed() {
        return coinsUsed;
    }

    public void setCoinsUsed(Long coinsUsed) {
        this.coinsUsed = coinsUsed;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Refund field getters and setters
    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public Long getCoinsRefunded() {
        return coinsRefunded;
    }

    public void setCoinsRefunded(Long coinsRefunded) {
        this.coinsRefunded = coinsRefunded;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public String getRefundType() {
        return refundType;
    }

    public void setRefundType(String refundType) {
        this.refundType = refundType;
    }

    public LocalDateTime getRefundRequestedAt() {
        return refundRequestedAt;
    }

    public void setRefundRequestedAt(LocalDateTime refundRequestedAt) {
        this.refundRequestedAt = refundRequestedAt;
    }

    public LocalDateTime getRefundCompletedAt() {
        return refundCompletedAt;
    }

    public void setRefundCompletedAt(LocalDateTime refundCompletedAt) {
        this.refundCompletedAt = refundCompletedAt;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public void setCanCancel(boolean canCancel) {
        this.canCancel = canCancel;
    }

    public boolean isCanReturn() {
        return canReturn;
    }

    public void setCanReturn(boolean canReturn) {
        this.canReturn = canReturn;
    }
}

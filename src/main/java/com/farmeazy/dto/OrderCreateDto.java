package com.farmeazy.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

public class OrderCreateDto {
    @NotEmpty(message = "Cart cannot be empty")
    private List<OrderItemDto> items;

    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Subtotal must be greater than 0")
    private BigDecimal subtotal;

    @NotNull(message = "Tax amount is required")
    @DecimalMin(value = "0.0", message = "Tax cannot be negative")
    private BigDecimal taxAmount;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total must be greater than 0")
    private BigDecimal totalAmount;

    private Long coinsUsed = 0L;

    @NotNull(message = "Final amount is required")
    @DecimalMin(value = "0.0", message = "Final amount cannot be negative")
    private BigDecimal finalAmount;

    @NotNull(message = "Payment method is required")
    private String paymentMethod; // UPI, PHONEPAY, CARD, CASH_ON_DELIVERY

    private Long addressId; // Required for CASH_ON_DELIVERY

    private AddressDto newAddress; // If address not in system

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    public OrderCreateDto() {
    }

    public OrderCreateDto(List<OrderItemDto> items, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal totalAmount, Long coinsUsed, BigDecimal finalAmount, String paymentMethod, Long addressId, AddressDto newAddress, String notes) {
        this.items = items;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.coinsUsed = coinsUsed;
        this.finalAmount = finalAmount;
        this.paymentMethod = paymentMethod;
        this.addressId = addressId;
        this.newAddress = newAddress;
        this.notes = notes;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
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

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public AddressDto getNewAddress() {
        return newAddress;
    }

    public void setNewAddress(AddressDto newAddress) {
        this.newAddress = newAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}


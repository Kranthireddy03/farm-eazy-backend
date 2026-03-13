package com.farmeazy.dto;

import java.math.BigDecimal;

public class OrderItemDetailDto {
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private java.util.List<String> mediaUrls;

    public OrderItemDetailDto() {
    }

    public OrderItemDetailDto(Long productId, String productName, int quantity, BigDecimal price, BigDecimal totalPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = totalPrice;
        this.mediaUrls = new java.util.ArrayList<>();
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public java.util.List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(java.util.List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}

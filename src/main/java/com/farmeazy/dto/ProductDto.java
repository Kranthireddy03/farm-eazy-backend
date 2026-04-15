package com.farmeazy.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ProductDto {
    
    private Long id;
    private Long sellerId;
    private String sellerUsername;
    private String sellerFullName;
    private String sellerEmail;
    private String sellerPhone;
    private String sellerLocation;
    private String productName;
    private String category;
    private String description;
    private Double price;
    private Double discountPercentage;
    private Double discountedPrice;
    private Integer quantity;
    private String unit;
    private String weight;
    private String specifications;
    private String warrantyInfo;
    private Integer deliveryDaysMin;
    private Integer deliveryDaysMax;
    private Long deliveryLocationId;
    private String deliveryLocationName;
    private String deliveryLocationCity;
    private String deliveryLocationState;
    private String deliveryLocationPostalCode;
    private java.math.BigDecimal deliveryLocationRadiusKm;
    private boolean deliverable;
    private String deliveryMessage;
    private String status;
    private String imageUrls; // Comma-separated image URLs for frontend
    private String videoUrls; // Comma-separated video URLs for frontend
    private String contactEmail;
    private String contactPhone;
    private List<String> mediaUrls;
    private Long vendorId;
    private String vendorName;
    private String vendorLocation;
    private String vendorType;
        public String getImageUrls() {
            return imageUrls;
        }
        public void setImageUrls(String imageUrls) {
            this.imageUrls = imageUrls;
        }
        public String getVideoUrls() {
            return videoUrls;
        }
        public void setVideoUrls(String videoUrls) {
            this.videoUrls = videoUrls;
        }
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }
    
    public String getSellerUsername() {
        return sellerUsername;
    }
    
    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }
    
    public String getSellerFullName() {
        return sellerFullName;
    }
    
    public void setSellerFullName(String sellerFullName) {
        this.sellerFullName = sellerFullName;
    }
    
    public String getSellerEmail() {
        return sellerEmail;
    }
    
    public void setSellerEmail(String sellerEmail) {
        this.sellerEmail = sellerEmail;
    }
    
    public String getSellerPhone() {
        return sellerPhone;
    }
    
    public void setSellerPhone(String sellerPhone) {
        this.sellerPhone = sellerPhone;
    }
    
    public String getSellerLocation() {
        return sellerLocation;
    }
    
    public void setSellerLocation(String sellerLocation) {
        this.sellerLocation = sellerLocation;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Double getPrice() {
        return price;
    }
    
    public void setPrice(Double price) {
        this.price = price;
    }
    
    public Double getDiscountPercentage() {
        return discountPercentage;
    }
    
    public void setDiscountPercentage(Double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }
    
    public Double getDiscountedPrice() {
        return discountedPrice;
    }
    
    public void setDiscountedPrice(Double discountedPrice) {
        this.discountedPrice = discountedPrice;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public String getWeight() {
        return weight;
    }
    
    public void setWeight(String weight) {
        this.weight = weight;
    }
    
    public String getSpecifications() {
        return specifications;
    }
    
    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }
    
    public String getWarrantyInfo() {
        return warrantyInfo;
    }
    
    public void setWarrantyInfo(String warrantyInfo) {
        this.warrantyInfo = warrantyInfo;
    }

    public Integer getDeliveryDaysMin() {
        return deliveryDaysMin;
    }

    public void setDeliveryDaysMin(Integer deliveryDaysMin) {
        this.deliveryDaysMin = deliveryDaysMin;
    }

    public Integer getDeliveryDaysMax() {
        return deliveryDaysMax;
    }

    public void setDeliveryDaysMax(Integer deliveryDaysMax) {
        this.deliveryDaysMax = deliveryDaysMax;
    }

    public Long getDeliveryLocationId() {
        return deliveryLocationId;
    }

    public void setDeliveryLocationId(Long deliveryLocationId) {
        this.deliveryLocationId = deliveryLocationId;
    }

    public String getDeliveryLocationName() {
        return deliveryLocationName;
    }

    public void setDeliveryLocationName(String deliveryLocationName) {
        this.deliveryLocationName = deliveryLocationName;
    }

    public String getDeliveryLocationCity() {
        return deliveryLocationCity;
    }

    public void setDeliveryLocationCity(String deliveryLocationCity) {
        this.deliveryLocationCity = deliveryLocationCity;
    }

    public String getDeliveryLocationState() {
        return deliveryLocationState;
    }

    public void setDeliveryLocationState(String deliveryLocationState) {
        this.deliveryLocationState = deliveryLocationState;
    }

    public String getDeliveryLocationPostalCode() {
        return deliveryLocationPostalCode;
    }

    public void setDeliveryLocationPostalCode(String deliveryLocationPostalCode) {
        this.deliveryLocationPostalCode = deliveryLocationPostalCode;
    }

    public java.math.BigDecimal getDeliveryLocationRadiusKm() {
        return deliveryLocationRadiusKm;
    }

    public void setDeliveryLocationRadiusKm(java.math.BigDecimal deliveryLocationRadiusKm) {
        this.deliveryLocationRadiusKm = deliveryLocationRadiusKm;
    }

    public boolean isDeliverable() {
        return deliverable;
    }

    public void setDeliverable(boolean deliverable) {
        this.deliverable = deliverable;
    }

    public String getDeliveryMessage() {
        return deliveryMessage;
    }

    public void setDeliveryMessage(String deliveryMessage) {
        this.deliveryMessage = deliveryMessage;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

        public Long getVendorId() {
            return vendorId;
        }
        public void setVendorId(Long vendorId) {
            this.vendorId = vendorId;
        }
        public String getVendorName() {
            return vendorName;
        }
        public void setVendorName(String vendorName) {
            this.vendorName = vendorName;
        }
        public String getVendorLocation() {
            return vendorLocation;
        }
        public void setVendorLocation(String vendorLocation) {
            this.vendorLocation = vendorLocation;
        }
        public String getVendorType() {
            return vendorType;
        }
        public void setVendorType(String vendorType) {
            this.vendorType = vendorType;
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
}

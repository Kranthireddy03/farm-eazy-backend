package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {
        @Column(name = "image_urls", columnDefinition = "TEXT")
        private String imageUrls;
        @Column(name = "video_urls", columnDefinition = "TEXT")
        private String videoUrls;
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
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(nullable = false)
    private String category;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Double price;
    
    @Column(name = "discount_percentage")
    private Double discountPercentage;
    
    @Column(name = "discounted_price")
    private Double discountedPrice;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private String unit;
    
    private String weight;
    
    @Column(columnDefinition = "TEXT")
    private String specifications;
    
    @Column(name = "warranty_info")
    private String warrantyInfo;
    
    @Column(nullable = false)
    private String status; // ACTIVE, OUT_OF_STOCK, DISCONTINUED
    
    // Removed imageUrls and videoUrls; use ProductMedia

        // Vendor Transparency Fields
        @Column(name = "vendor_id")
        private Long vendorId;
        @Column(name = "vendor_name")
        private String vendorName;
        @Column(name = "vendor_location")
        private String vendorLocation;
        @Column(name = "vendor_type")
        private String vendorType;
        
            // Vendor Transparency Getters/Setters
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

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductMedia> mediaFiles = new ArrayList<>();

    @Column(name = "seller_phone")
    private String sellerPhone;

    @Column(name = "seller_email")
    private String sellerEmail;

    // Marketplace / Payout fields
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", length = 50)
    private PayoutStatus payoutStatus = PayoutStatus.NOT_APPLICABLE;

    @Column(name = "platform_fee_percentage", precision = 5, scale = 2)
    private java.math.BigDecimal platformFeePercentage = new java.math.BigDecimal("5.00");
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (discountPercentage != null && discountPercentage > 0) {
            discountedPrice = price - (price * discountPercentage / 100);
        } else {
            discountedPrice = price;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (discountPercentage != null && discountPercentage > 0) {
            discountedPrice = price - (price * discountPercentage / 100);
        } else {
            discountedPrice = price;
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.productName;
    }
    
    public User getSeller() {
        return seller;
    }
    
    public void setSeller(User seller) {
        this.seller = seller;
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

    public List<ProductMedia> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<ProductMedia> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    public String getSellerPhone() {
        return sellerPhone;
    }

    public void setSellerPhone(String sellerPhone) {
        this.sellerPhone = sellerPhone;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }

    public void setSellerEmail(String sellerEmail) {
        this.sellerEmail = sellerEmail;
    }

    public PayoutStatus getPayoutStatus() {
        return payoutStatus;
    }

    public void setPayoutStatus(PayoutStatus payoutStatus) {
        this.payoutStatus = payoutStatus;
    }

    public java.math.BigDecimal getPlatformFeePercentage() {
        return platformFeePercentage;
    }

    public void setPlatformFeePercentage(java.math.BigDecimal platformFeePercentage) {
        this.platformFeePercentage = platformFeePercentage;
    }

    public enum PayoutStatus {
        NOT_APPLICABLE,
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}

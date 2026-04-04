package com.farmeazy.dto;

import jakarta.validation.constraints.*;

public class ProductCreateDto {
                // Seller Transparency Fields
                @NotBlank(message = "Seller email is required")
                @Email(message = "Invalid seller email format")
                private String sellerEmail;

                @Pattern(regexp = "^[0-9]{10}$", message = "Seller phone must be 10 digits")
                private String sellerPhone;
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
    
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 255, message = "Product name must be between 3 and 255 characters")
    private String productName;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    private String description;
    
    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be positive")
    private Double price;
    
    @Min(value = 0, message = "Discount percentage must be between 0 and 100")
    @Max(value = 100, message = "Discount percentage must be between 0 and 100")
    private Double discountPercentage;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @NotBlank(message = "Unit is required")
    private String unit;
    
    private String weight;
    private String specifications;
    private String warrantyInfo;
    @Min(value = 1, message = "Minimum delivery days must be at least 1")
    private Integer deliveryDaysMin = 3;
    @Min(value = 1, message = "Maximum delivery days must be at least 1")
    private Integer deliveryDaysMax = 5;
    // Removed imageUrls and videoUrls; only upload files

        // Vendor Transparency Fields
        private Long vendorId;
        private String vendorName;
        @NotBlank(message = "Vendor location is required")
        private String vendorLocation;
        @NotBlank(message = "Vendor type is required")
        private String vendorType;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid email format")
    private String contactEmail;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String contactPhone;
    
    // Getters and Setters
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
}

package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to store dynamic attributes for services.
 * Allows flexible service-specific fields based on service type.
 */
@Entity
@Table(name = "service_attributes")
public class ServiceAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_listing_id", nullable = false)
    private ServiceListing serviceListing;

    @Column(name = "attribute_key", nullable = false, length = 100)
    private String attributeKey;

    @Column(name = "attribute_value", nullable = false, columnDefinition = "TEXT")
    private String attributeValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "attribute_type", length = 50)
    private AttributeType attributeType = AttributeType.STRING;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AttributeType {
        STRING,
        NUMBER,
        BOOLEAN,
        DATE,
        LIST,
        JSON
    }

    public ServiceAttribute() {
    }

    public ServiceAttribute(ServiceListing serviceListing, String attributeKey, 
                           String attributeValue, AttributeType attributeType) {
        this.serviceListing = serviceListing;
        this.attributeKey = attributeKey;
        this.attributeValue = attributeValue;
        this.attributeType = attributeType;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceListing getServiceListing() {
        return serviceListing;
    }

    public void setServiceListing(ServiceListing serviceListing) {
        this.serviceListing = serviceListing;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public AttributeType getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(AttributeType attributeType) {
        this.attributeType = attributeType;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get value as integer (for NUMBER type)
     */
    public Integer getValueAsInteger() {
        try {
            return Integer.parseInt(attributeValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get value as boolean (for BOOLEAN type)
     */
    public Boolean getValueAsBoolean() {
        return Boolean.parseBoolean(attributeValue);
    }
}

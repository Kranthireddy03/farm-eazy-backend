package com.farmeazy.dto;

import com.farmeazy.entity.ServiceListing.ServiceType;

public class ServiceListingDto {
        private Long vendorId;
        private String vendorName;
        private String vendorLocation;
        private String vendorType;
    private Long id;
    private String serviceName;
    private String description;
    private double price;
    private Long providerId;
    private ServiceType type;
    private String location;
    private String availability;
    private String contactName;
    private String contactPhone;
    private String contactEmail;

    public ServiceListingDto() {
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

    public ServiceListingDto(Long id, String serviceName, String description, double price, Long providerId,
                             ServiceType type, String location, String availability, String contactName,
                             String contactPhone, String contactEmail) {
        this.id = id;
        this.serviceName = serviceName;
        this.description = description;
        this.price = price;
        this.providerId = providerId;
        this.type = type;
        this.location = location;
        this.availability = availability;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public ServiceType getType() {
        return type;
    }

    public void setType(ServiceType type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}

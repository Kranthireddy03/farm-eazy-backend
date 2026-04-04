package com.farmeazy.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Enhanced DTO for Service Listing with marketplace features.
 */
public class ServiceListingCreateDto {

    @NotBlank(message = "Service type is required")
    private String type; // TRACTOR, JCB, MANUAL, IRRIGATION, HARVESTER, SPRAYER, TRANSPORT

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.01", message = "Rate must be greater than 0")
    private Double rate;

    // Enhanced pricing
    private BigDecimal basePrice;
    private Boolean hasDriver = false;
    private BigDecimal driverPrice;
    private BigDecimal machinePrice;
    private String priceUnit = "PER_HOUR"; // PER_HOUR, PER_DAY, PER_ACRE, PER_HECTARE, FIXED

    // Equipment details (for TRACTOR, JCB, HARVESTER)
    private Boolean fuelIncluded = false;
    private Boolean operatorIncluded = false;
    private Integer minimumHours = 1;
    private Integer maximumHours;
    private Integer serviceRadiusKm = 50;
    private String equipmentPower; // HP for tractors
    private String equipmentModel;
    private List<String> implementsAvailable; // List of available implements

    // Manual labor specific
    private Integer workersCount;
    private Boolean toolsIncluded = false;
    private Integer experienceYears;

    // Contact details
    @NotBlank(message = "Contact name is required")
    private String contactName;

    @NotBlank(message = "Contact phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String contactPhone;

    @Email(message = "Invalid email format")
    private String contactEmail;

        // Vendor Transparency Fields
        private Long vendorId;
        private String vendorName;
        @NotBlank(message = "Vendor location is required")
        private String vendorLocation;
        @NotBlank(message = "Vendor type is required")
        private String vendorType;

    @NotBlank(message = "Availability is required")
    private String availability;

    // Dynamic attributes for service-specific fields
    private Map<String, Object> customAttributes;

    public ServiceListingCreateDto() {
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Boolean getHasDriver() {
        return hasDriver;
    }

    public void setHasDriver(Boolean hasDriver) {
        this.hasDriver = hasDriver;
    }

    public BigDecimal getDriverPrice() {
        return driverPrice;
    }

    public void setDriverPrice(BigDecimal driverPrice) {
        this.driverPrice = driverPrice;
    }

    public BigDecimal getMachinePrice() {
        return machinePrice;
    }

    public void setMachinePrice(BigDecimal machinePrice) {
        this.machinePrice = machinePrice;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit) {
        this.priceUnit = priceUnit;
    }

    public Boolean getFuelIncluded() {
        return fuelIncluded;
    }

    public void setFuelIncluded(Boolean fuelIncluded) {
        this.fuelIncluded = fuelIncluded;
    }

    public Boolean getOperatorIncluded() {
        return operatorIncluded;
    }

    public void setOperatorIncluded(Boolean operatorIncluded) {
        this.operatorIncluded = operatorIncluded;
    }

    public Integer getMinimumHours() {
        return minimumHours;
    }

    public void setMinimumHours(Integer minimumHours) {
        this.minimumHours = minimumHours;
    }

    public Integer getMaximumHours() {
        return maximumHours;
    }

    public void setMaximumHours(Integer maximumHours) {
        this.maximumHours = maximumHours;
    }

    public Integer getServiceRadiusKm() {
        return serviceRadiusKm;
    }

    public void setServiceRadiusKm(Integer serviceRadiusKm) {
        this.serviceRadiusKm = serviceRadiusKm;
    }

    public String getEquipmentPower() {
        return equipmentPower;
    }

    public void setEquipmentPower(String equipmentPower) {
        this.equipmentPower = equipmentPower;
    }

    public String getEquipmentModel() {
        return equipmentModel;
    }

    public void setEquipmentModel(String equipmentModel) {
        this.equipmentModel = equipmentModel;
    }

    public List<String> getImplementsAvailable() {
        return implementsAvailable;
    }

    public void setImplementsAvailable(List<String> implementsAvailable) {
        this.implementsAvailable = implementsAvailable;
    }

    public Integer getWorkersCount() {
        return workersCount;
    }

    public void setWorkersCount(Integer workersCount) {
        this.workersCount = workersCount;
    }

    public Boolean getToolsIncluded() {
        return toolsIncluded;
    }

    public void setToolsIncluded(Boolean toolsIncluded) {
        this.toolsIncluded = toolsIncluded;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
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

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }
}

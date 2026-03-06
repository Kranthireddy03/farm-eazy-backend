package com.farmeazy.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for Service Listing with all enhanced fields.
 */
public class ServiceListingResponseDto {

    private Long id;
    private String type;
    private String title;
    private String description;
    private String location;
    private Double rate;

    // Enhanced pricing
    private BigDecimal basePrice;
    private Boolean hasDriver;
    private BigDecimal driverPrice;
    private BigDecimal machinePrice;
    private BigDecimal totalPrice; // Calculated: machine + driver (if applicable)
    private String priceUnit;

    // Equipment details
    private Boolean fuelIncluded;
    private Boolean operatorIncluded;
    private Integer minimumHours;
    private Integer maximumHours;
    private Integer serviceRadiusKm;
    private String equipmentPower;
    private String equipmentModel;
    private List<String> implementsAvailable;

    // Manual labor specific
    private Integer workersCount;
    private Boolean toolsIncluded;
    private Integer experienceYears;

    // Status
    private Boolean isActive;
    private String payoutStatus;

    // Contact details
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String availability;

    // Provider details
    private Long userId;
    private String userName;
    private String userFullName;

    // Dynamic attributes
    private Map<String, Object> customAttributes;

    private String createdAt;
    private String updatedAt;

    public ServiceListingResponseDto() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getPayoutStatus() {
        return payoutStatus;
    }

    public void setPayoutStatus(String payoutStatus) {
        this.payoutStatus = payoutStatus;
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

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}

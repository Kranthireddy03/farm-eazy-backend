package com.farmeazy.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Enhanced DTO for Service Booking with payment details.
 */
public class ServiceBookingCreateDto {

    @NotNull(message = "Service listing ID is required")
    private Long serviceListingId;

    @NotNull(message = "Farm ID is required")
    private Long farmId;

    private Long cropId;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Service date is required")
    private LocalDate serviceDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @NotNull(message = "Number of hours is required")
    @Min(value = 1, message = "Hours must be at least 1")
    private Integer hours;

    private Integer peopleCount; // For manual labor

    private String notes;

    // Pricing (calculated on backend, but can be passed for verification)
    private BigDecimal machineAmount;
    private BigDecimal driverAmount;
    private BigDecimal labourAmount;
    private Boolean includeDriver = false;

    // Payment method
    private String paymentMethod; // RAZORPAY, COD (Cash on Delivery)

    public ServiceBookingCreateDto() {
    }

    // Getters and Setters
    public Long getServiceListingId() {
        return serviceListingId;
    }

    public void setServiceListingId(Long serviceListingId) {
        this.serviceListingId = serviceListingId;
    }

    public Long getFarmId() {
        return farmId;
    }

    public void setFarmId(Long farmId) {
        this.farmId = farmId;
    }

    public Long getCropId() {
        return cropId;
    }

    public void setCropId(Long cropId) {
        this.cropId = cropId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getHours() {
        return hours;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getMachineAmount() {
        return machineAmount;
    }

    public void setMachineAmount(BigDecimal machineAmount) {
        this.machineAmount = machineAmount;
    }

    public BigDecimal getDriverAmount() {
        return driverAmount;
    }

    public void setDriverAmount(BigDecimal driverAmount) {
        this.driverAmount = driverAmount;
    }

    public BigDecimal getLabourAmount() {
        return labourAmount;
    }

    public void setLabourAmount(BigDecimal labourAmount) {
        this.labourAmount = labourAmount;
    }

    public Boolean getIncludeDriver() {
        return includeDriver;
    }

    public void setIncludeDriver(Boolean includeDriver) {
        this.includeDriver = includeDriver;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}

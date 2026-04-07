package com.farmeazy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayoutBatchListDto {

    private Long id;
    private LocalDate batchDate;
    private Integer vendorCount;
    private BigDecimal totalAmount;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private String approvedBy;
    private LocalDateTime approvedAt;

    public PayoutBatchListDto() {
    }

    public PayoutBatchListDto(Long id, LocalDate batchDate, Integer vendorCount, BigDecimal totalAmount,
                              String status, String createdBy, LocalDateTime createdAt,
                              String approvedBy, LocalDateTime approvedAt) {
        this.id = id;
        this.batchDate = batchDate;
        this.vendorCount = vendorCount;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getBatchDate() {
        return batchDate;
    }

    public void setBatchDate(LocalDate batchDate) {
        this.batchDate = batchDate;
    }

    public Integer getVendorCount() {
        return vendorCount;
    }

    public void setVendorCount(Integer vendorCount) {
        this.vendorCount = vendorCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}

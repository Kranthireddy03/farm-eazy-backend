package com.farmeazy.dto;

import java.math.BigDecimal;

public class PayoutDetailDto {

    private Long id;
    private Long batchId;
    private Long vendorId;
    private String vendorName;
    private String vendorEmail;
    private BigDecimal amount;
    private String status;
    private String transactionReference;
    private String razorpayPayoutId;
    private String accountNumberMasked;
    private String ifscCode;
    private String accountHolderName;
    private String failureReason;
    private Integer retryCount;
    private Integer maxRetries;

    public PayoutDetailDto() {
    }

    public PayoutDetailDto(Long id, Long batchId, Long vendorId, String vendorName, String vendorEmail,
                           BigDecimal amount, String status, String transactionReference, String razorpayPayoutId,
                           String accountNumberMasked, String ifscCode, String accountHolderName,
                           String failureReason, Integer retryCount, Integer maxRetries) {
        this.id = id;
        this.batchId = batchId;
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.vendorEmail = vendorEmail;
        this.amount = amount;
        this.status = status;
        this.transactionReference = transactionReference;
        this.razorpayPayoutId = razorpayPayoutId;
        this.accountNumberMasked = accountNumberMasked;
        this.ifscCode = ifscCode;
        this.accountHolderName = accountHolderName;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
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

    public String getVendorEmail() {
        return vendorEmail;
    }

    public void setVendorEmail(String vendorEmail) {
        this.vendorEmail = vendorEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getRazorpayPayoutId() {
        return razorpayPayoutId;
    }

    public void setRazorpayPayoutId(String razorpayPayoutId) {
        this.razorpayPayoutId = razorpayPayoutId;
    }

    public String getAccountNumberMasked() {
        return accountNumberMasked;
    }

    public void setAccountNumberMasked(String accountNumberMasked) {
        this.accountNumberMasked = accountNumberMasked;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
}

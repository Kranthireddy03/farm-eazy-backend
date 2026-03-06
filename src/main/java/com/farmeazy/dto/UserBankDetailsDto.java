package com.farmeazy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for User Bank Details - used for seller payouts.
 */
public class UserBankDetailsDto {

    private Long id;

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 255, message = "Account holder name must be between 2 and 255 characters")
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    @Size(min = 9, max = 18, message = "Account number must be between 9 and 18 digits")
    @Pattern(regexp = "^[0-9]+$", message = "Account number must contain only digits")
    private String accountNumber;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
    private String ifscCode;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    private String branchName;

    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z][a-zA-Z]{2,64}$", message = "Invalid UPI ID format")
    private String upiId;

    private Boolean isVerified;
    private Boolean isPrimary;
    private String maskedAccountNumber;
    private String createdAt;
    private String updatedAt;

    // Security fields
    private Integer changeCount;
    private Integer remainingChanges;
    private Boolean canChange;
    private Boolean hasSecurityQuestion;
    private String securityQuestion;
    private String securityAnswer; // Only used for input, never returned

    public UserBankDetailsDto() {
    }

    public UserBankDetailsDto(String accountHolderName, String accountNumber, String ifscCode, String bankName) {
        this.accountHolderName = accountHolderName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.bankName = bankName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }

    public void setMaskedAccountNumber(String maskedAccountNumber) {
        this.maskedAccountNumber = maskedAccountNumber;
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

    public Integer getChangeCount() {
        return changeCount;
    }

    public void setChangeCount(Integer changeCount) {
        this.changeCount = changeCount;
    }

    public Integer getRemainingChanges() {
        return remainingChanges;
    }

    public void setRemainingChanges(Integer remainingChanges) {
        this.remainingChanges = remainingChanges;
    }

    public Boolean getCanChange() {
        return canChange;
    }

    public void setCanChange(Boolean canChange) {
        this.canChange = canChange;
    }

    public Boolean getHasSecurityQuestion() {
        return hasSecurityQuestion;
    }

    public void setHasSecurityQuestion(Boolean hasSecurityQuestion) {
        this.hasSecurityQuestion = hasSecurityQuestion;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }
}

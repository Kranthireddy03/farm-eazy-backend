package com.farmeazy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * BANK VERIFICATION REQUEST DTO
 * 
 * Used for initiating bank account/UPI verification.
 */
public class BankVerificationRequestDto {

    @NotBlank(message = "Verification type is required")
    @Pattern(regexp = "BANK_ACCOUNT|UPI", message = "Verification type must be BANK_ACCOUNT or UPI")
    private String verificationType;

    // Bank account fields
    private String accountHolderName;

    @Pattern(regexp = "^[0-9]{9,18}$", message = "Invalid account number")
    private String accountNumber;

    private String confirmAccountNumber;

    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
    private String ifscCode;

    private String bankName;

    private String branchName;

    // UPI fields
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]+@[a-zA-Z]+$", message = "Invalid UPI ID format")
    private String upiId;

    // Constructors
    public BankVerificationRequestDto() {
    }

    public BankVerificationRequestDto(String verificationType, String accountHolderName, String accountNumber,
            String confirmAccountNumber, String ifscCode, String bankName, String branchName, String upiId) {
        this.verificationType = verificationType;
        this.accountHolderName = accountHolderName;
        this.accountNumber = accountNumber;
        this.confirmAccountNumber = confirmAccountNumber;
        this.ifscCode = ifscCode;
        this.bankName = bankName;
        this.branchName = branchName;
        this.upiId = upiId;
    }

    // Getters and Setters
    public String getVerificationType() {
        return verificationType;
    }

    public void setVerificationType(String verificationType) {
        this.verificationType = verificationType;
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

    public String getConfirmAccountNumber() {
        return confirmAccountNumber;
    }

    public void setConfirmAccountNumber(String confirmAccountNumber) {
        this.confirmAccountNumber = confirmAccountNumber;
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
}

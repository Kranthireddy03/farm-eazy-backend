package com.farmeazy.dto;

import java.math.BigDecimal;

/**
 * BANK VERIFICATION RESPONSE DTO
 * 
 * Used for returning verification status and limits to client.
 */
public class BankVerificationResponseDto {

    private boolean success;
    private String verificationNumber;
    private String status;
    private BigDecimal transferAmount;
    private String transferReferenceId;
    private String message;

    // Rate limit information
    private boolean canVerify;
    private int remainingToday;
    private int remainingThisMonth;
    private int remainingTotal;
    private BigDecimal totalSpent;
    private boolean blocked;
    private String blockedReason;

    // Constructors
    public BankVerificationResponseDto() {
    }

    public BankVerificationResponseDto(boolean success, String verificationNumber, String status,
            BigDecimal transferAmount, String transferReferenceId, String message, boolean canVerify,
            int remainingToday, int remainingThisMonth, int remainingTotal, BigDecimal totalSpent, boolean blocked,
            String blockedReason) {
        this.success = success;
        this.verificationNumber = verificationNumber;
        this.status = status;
        this.transferAmount = transferAmount;
        this.transferReferenceId = transferReferenceId;
        this.message = message;
        this.canVerify = canVerify;
        this.remainingToday = remainingToday;
        this.remainingThisMonth = remainingThisMonth;
        this.remainingTotal = remainingTotal;
        this.totalSpent = totalSpent;
        this.blocked = blocked;
        this.blockedReason = blockedReason;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getVerificationNumber() {
        return verificationNumber;
    }

    public void setVerificationNumber(String verificationNumber) {
        this.verificationNumber = verificationNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTransferAmount() {
        return transferAmount;
    }

    public void setTransferAmount(BigDecimal transferAmount) {
        this.transferAmount = transferAmount;
    }

    public String getTransferReferenceId() {
        return transferReferenceId;
    }

    public void setTransferReferenceId(String transferReferenceId) {
        this.transferReferenceId = transferReferenceId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCanVerify() {
        return canVerify;
    }

    public void setCanVerify(boolean canVerify) {
        this.canVerify = canVerify;
    }

    public int getRemainingToday() {
        return remainingToday;
    }

    public void setRemainingToday(int remainingToday) {
        this.remainingToday = remainingToday;
    }

    public int getRemainingThisMonth() {
        return remainingThisMonth;
    }

    public void setRemainingThisMonth(int remainingThisMonth) {
        this.remainingThisMonth = remainingThisMonth;
    }

    public int getRemainingTotal() {
        return remainingTotal;
    }

    public void setRemainingTotal(int remainingTotal) {
        this.remainingTotal = remainingTotal;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public void setBlockedReason(String blockedReason) {
        this.blockedReason = blockedReason;
    }
}

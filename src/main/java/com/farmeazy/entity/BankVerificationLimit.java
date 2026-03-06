package com.farmeazy.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BANK VERIFICATION LIMIT ENTITY
 * 
 * PURPOSE: Tracks verification limits per user to prevent abuse of the
 * 1 rupee verification system. Implements professional rate limiting.
 * 
 * KEY FEATURES:
 * - Daily limit (default 3) to prevent excessive attempts
 * - Monthly limit (default 10) for medium-term control
 * - Total lifetime limit (default 50) for long-term abuse prevention
 * - Tracks total amount spent on verification
 * - Blocking mechanism for suspicious activity
 * 
 * BUSINESS RULE:
 * If a user changes bank details 100 times, we should NOT send ₹100.
 * This entity ensures professional handling with configurable limits.
 */
@Entity
@Table(name = "bank_verification_limit")
public class BankVerificationLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "daily_limit")
    private Integer dailyLimit = 3;

    @Column(name = "monthly_limit")
    private Integer monthlyLimit = 10;

    @Column(name = "total_limit")
    private Integer totalLimit = 50;

    @Column(name = "used_today")
    private Integer usedToday = 0;

    @Column(name = "used_this_month")
    private Integer usedThisMonth = 0;

    @Column(name = "total_used")
    private Integer totalUsed = 0;

    @Column(name = "total_amount_spent", precision = 10, scale = 2)
    private BigDecimal totalAmountSpent = BigDecimal.ZERO;

    @Column(name = "last_verification_date")
    private LocalDate lastVerificationDate;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate;

    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    @Column(name = "blocked_reason", length = 255)
    private String blockedReason;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "unblocked_at")
    private LocalDateTime unblockedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public BankVerificationLimit() {
    }

    public BankVerificationLimit(Long id, User user, Integer dailyLimit, Integer monthlyLimit, Integer totalLimit,
            Integer usedToday, Integer usedThisMonth, Integer totalUsed, BigDecimal totalAmountSpent,
            LocalDate lastVerificationDate, LocalDate lastResetDate, Boolean isBlocked, String blockedReason,
            LocalDateTime blockedAt, LocalDateTime unblockedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.totalLimit = totalLimit;
        this.usedToday = usedToday;
        this.usedThisMonth = usedThisMonth;
        this.totalUsed = totalUsed;
        this.totalAmountSpent = totalAmountSpent;
        this.lastVerificationDate = lastVerificationDate;
        this.lastResetDate = lastResetDate;
        this.isBlocked = isBlocked;
        this.blockedReason = blockedReason;
        this.blockedAt = blockedAt;
        this.unblockedAt = unblockedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(Integer dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Integer getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(Integer monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public Integer getTotalLimit() {
        return totalLimit;
    }

    public void setTotalLimit(Integer totalLimit) {
        this.totalLimit = totalLimit;
    }

    public Integer getUsedToday() {
        return usedToday;
    }

    public void setUsedToday(Integer usedToday) {
        this.usedToday = usedToday;
    }

    public Integer getUsedThisMonth() {
        return usedThisMonth;
    }

    public void setUsedThisMonth(Integer usedThisMonth) {
        this.usedThisMonth = usedThisMonth;
    }

    public Integer getTotalUsed() {
        return totalUsed;
    }

    public void setTotalUsed(Integer totalUsed) {
        this.totalUsed = totalUsed;
    }

    public BigDecimal getTotalAmountSpent() {
        return totalAmountSpent;
    }

    public void setTotalAmountSpent(BigDecimal totalAmountSpent) {
        this.totalAmountSpent = totalAmountSpent;
    }

    public LocalDate getLastVerificationDate() {
        return lastVerificationDate;
    }

    public void setLastVerificationDate(LocalDate lastVerificationDate) {
        this.lastVerificationDate = lastVerificationDate;
    }

    public LocalDate getLastResetDate() {
        return lastResetDate;
    }

    public void setLastResetDate(LocalDate lastResetDate) {
        this.lastResetDate = lastResetDate;
    }

    public Boolean getIsBlocked() {
        return isBlocked;
    }

    public void setIsBlocked(Boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public void setBlockedReason(String blockedReason) {
        this.blockedReason = blockedReason;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }

    public LocalDateTime getUnblockedAt() {
        return unblockedAt;
    }

    public void setUnblockedAt(LocalDateTime unblockedAt) {
        this.unblockedAt = unblockedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Checks if user can perform verification based on limits.
     * @return true if verification is allowed, false otherwise
     */
    public boolean canVerify() {
        if (isBlocked) {
            return false;
        }
        
        // Reset daily counter if it's a new day
        if (lastVerificationDate != null && !lastVerificationDate.equals(LocalDate.now())) {
            usedToday = 0;
        }
        
        // Check all limits
        return usedToday < dailyLimit && 
               usedThisMonth < monthlyLimit && 
               totalUsed < totalLimit;
    }

    /**
     * Gets the remaining verification attempts for today.
     */
    public int getRemainingToday() {
        if (lastVerificationDate != null && !lastVerificationDate.equals(LocalDate.now())) {
            return dailyLimit;
        }
        return Math.max(0, dailyLimit - usedToday);
    }

    /**
     * Gets the remaining verification attempts for this month.
     */
    public int getRemainingThisMonth() {
        return Math.max(0, monthlyLimit - usedThisMonth);
    }

    /**
     * Gets the remaining total verification attempts.
     */
    public int getRemainingTotal() {
        return Math.max(0, totalLimit - totalUsed);
    }

    /**
     * Increments usage counters after a verification attempt.
     */
    public void incrementUsage(BigDecimal amount) {
        // Reset daily counter if it's a new day
        if (lastVerificationDate != null && !lastVerificationDate.equals(LocalDate.now())) {
            usedToday = 0;
        }
        
        usedToday++;
        usedThisMonth++;
        totalUsed++;
        totalAmountSpent = totalAmountSpent.add(amount);
        lastVerificationDate = LocalDate.now();
    }

    /**
     * Resets monthly counters (called by monthly batch job).
     */
    public void resetMonthlyCounters() {
        usedThisMonth = 0;
        lastResetDate = LocalDate.now();
    }
}

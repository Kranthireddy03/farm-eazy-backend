package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_coins")
public class UserCoins {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(nullable = false)
    private Integer totalCoins = 0;
    
    @Column(nullable = false)
    private Integer coinsEarned = 0;
    
    @Column(nullable = false)
    private Integer coinsSpent = 0;
    
    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;
    
    @Column(name = "login_count_today", nullable = false)
    private Integer loginCountToday = 0;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public UserCoins() {}
    
    public UserCoins(User user, Integer initialCoins) {
        this.user = user;
        this.totalCoins = initialCoins;
        this.coinsEarned = initialCoins;
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
    
    public Integer getTotalCoins() {
        return totalCoins;
    }
    
    public void setTotalCoins(Integer totalCoins) {
        this.totalCoins = totalCoins;
    }
    
    public Integer getCoinsEarned() {
        return coinsEarned;
    }
    
    public void setCoinsEarned(Integer coinsEarned) {
        this.coinsEarned = coinsEarned;
    }
    
    public Integer getCoinsSpent() {
        return coinsSpent;
    }
    
    public void setCoinsSpent(Integer coinsSpent) {
        this.coinsSpent = coinsSpent;
    }
    
    public LocalDate getLastLoginDate() {
        return lastLoginDate;
    }
    
    public void setLastLoginDate(LocalDate lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
    
    public Integer getLoginCountToday() {
        return loginCountToday;
    }
    
    public void setLoginCountToday(Integer loginCountToday) {
        this.loginCountToday = loginCountToday;
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
}

package com.farmeazy.dto;

import java.time.LocalDate;

public class UserCoinsDto {
    private Long id;
    private Long userId;
    private Integer totalCoins;
    private Integer coinsEarned;
    private Integer coinsSpent;
    private LocalDate lastLoginDate;
    private Integer loginCountToday;
    private Integer dailyLoginCoinsAvailable;
    
    // Constructors
    public UserCoinsDto() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
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
    
    public Integer getDailyLoginCoinsAvailable() {
        return dailyLoginCoinsAvailable;
    }
    
    public void setDailyLoginCoinsAvailable(Integer dailyLoginCoinsAvailable) {
        this.dailyLoginCoinsAvailable = dailyLoginCoinsAvailable;
    }
}

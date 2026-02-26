package com.farmeazy.service;

import com.farmeazy.dto.UserCoinsDto;
import com.farmeazy.entity.User;
import com.farmeazy.entity.UserCoins;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.UserCoinsRepository;
import com.farmeazy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CoinService {

        @Autowired
        private HttpEmailService httpEmailService;
    
    private static final Integer INITIAL_COINS = 50;
    private static final Integer LOGIN_BONUS_COINS = 5;
    private static final Integer MAX_DAILY_LOGINS = 3;
    
    private final UserCoinsRepository userCoinsRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public CoinService(UserCoinsRepository userCoinsRepository, UserRepository userRepository) {
        this.userCoinsRepository = userCoinsRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional
    public UserCoinsDto getUserCoins(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        UserCoins userCoins = userCoinsRepository.findByUser(user)
            .orElseGet(() -> initializeUserCoins(user));
        
        return convertToDto(userCoins);
    }
    
    @Transactional
    public UserCoinsDto processLoginBonus(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        UserCoins userCoins = userCoinsRepository.findByUser(user)
            .orElseGet(() -> initializeUserCoins(user));
        
        LocalDate today = LocalDate.now();
        
        // Reset login count if it's a new day
        if (userCoins.getLastLoginDate() == null || !userCoins.getLastLoginDate().equals(today)) {
            userCoins.setLastLoginDate(today);
            userCoins.setLoginCountToday(0);
        }
        
        // Award login bonus if eligible (max 3 per day)
        if (userCoins.getLoginCountToday() < MAX_DAILY_LOGINS) {
            userCoins.setTotalCoins(userCoins.getTotalCoins() + LOGIN_BONUS_COINS);
            userCoins.setCoinsEarned(userCoins.getCoinsEarned() + LOGIN_BONUS_COINS);
            userCoins.setLoginCountToday(userCoins.getLoginCountToday() + 1);
            userCoinsRepository.save(userCoins);
        }
        
        return convertToDto(userCoins);
    }
    
    @Transactional
    public UserCoinsDto addCoins(String email, Integer amount, String reason) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        UserCoins userCoins = userCoinsRepository.findByUser(user)
            .orElseGet(() -> initializeUserCoins(user));
        
        userCoins.setTotalCoins(userCoins.getTotalCoins() + amount);
        userCoins.setCoinsEarned(userCoins.getCoinsEarned() + amount);
        
        userCoinsRepository.save(userCoins);

        // Send coin earn email notification
        try {
            // You may want to use a dedicated method in HttpEmailService for coin notifications
            httpEmailService.sendCoinEarnedNotification(
                user.getEmail(),
                user.getFullName(),
                amount,
                userCoins.getTotalCoins(),
                reason
            );
        } catch (Exception e) {
            System.err.println("Failed to send coin earned email: " + e.getMessage());
        }
        return convertToDto(userCoins);
    }
    
    @Transactional
    public UserCoinsDto deductCoins(String email, Integer amount) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        UserCoins userCoins = userCoinsRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("User coins not found"));
        
        if (userCoins.getTotalCoins() < amount) {
            throw new IllegalArgumentException("Insufficient coins. Available: " + userCoins.getTotalCoins() + ", Required: " + amount);
        }
        
        userCoins.setTotalCoins(userCoins.getTotalCoins() - amount);
        userCoins.setCoinsSpent(userCoins.getCoinsSpent() + amount);
        
        userCoinsRepository.save(userCoins);

        // Send coin spend email notification
        try {
            httpEmailService.sendCoinSpentNotification(
                user.getEmail(),
                user.getFullName(),
                amount,
                userCoins.getTotalCoins()
            );
        } catch (Exception e) {
            System.err.println("Failed to send coin spent email: " + e.getMessage());
        }
        return convertToDto(userCoins);
    }

    /**
     * Deduct coins from user (overloaded with User object)
     */
    @Transactional
    public UserCoinsDto deductCoins(User user, Long amount, String reason) {
        UserCoins userCoins = userCoinsRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("User coins not found"));
        
        if (userCoins.getTotalCoins() < amount) {
            throw new IllegalArgumentException("Insufficient coins. Available: " + userCoins.getTotalCoins() + ", Required: " + amount);
        }
        
        userCoins.setTotalCoins(userCoins.getTotalCoins() - amount.intValue());
        userCoins.setCoinsSpent(userCoins.getCoinsSpent() + amount.intValue());
        
        userCoinsRepository.save(userCoins);
        return convertToDto(userCoins);
    }

    /**
     * Add coins to user (overloaded with User object)
     */
    @Transactional
    public UserCoinsDto addCoins(User user, Long amount, String reason) {
        UserCoins userCoins = userCoinsRepository.findByUser(user)
            .orElseGet(() -> initializeUserCoins(user));
        
        userCoins.setTotalCoins(userCoins.getTotalCoins() + amount.intValue());
        userCoins.setCoinsEarned(userCoins.getCoinsEarned() + amount.intValue());
        
        userCoinsRepository.save(userCoins);
        return convertToDto(userCoins);
    }
    
    private UserCoins initializeUserCoins(User user) {
        UserCoins userCoins = new UserCoins(user, INITIAL_COINS);
        userCoins.setLastLoginDate(LocalDate.now());
        userCoins.setLoginCountToday(0);
        return userCoinsRepository.save(userCoins);
    }
    
    private UserCoinsDto convertToDto(UserCoins userCoins) {
        UserCoinsDto dto = new UserCoinsDto();
        dto.setId(userCoins.getId());
        dto.setUserId(userCoins.getUser().getId());
        dto.setTotalCoins(userCoins.getTotalCoins());
        dto.setCoinsEarned(userCoins.getCoinsEarned());
        dto.setCoinsSpent(userCoins.getCoinsSpent());
        dto.setLastLoginDate(userCoins.getLastLoginDate());
        dto.setLoginCountToday(userCoins.getLoginCountToday());
        
        // Calculate remaining daily login bonuses
        Integer remainingLogins = MAX_DAILY_LOGINS - userCoins.getLoginCountToday();
        dto.setDailyLoginCoinsAvailable(Math.max(0, remainingLogins));
        
        return dto;
    }
}

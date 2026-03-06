package com.farmeazy.controller;

import com.farmeazy.dto.PayoutDto;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing payouts.
 */
@RestController
@RequestMapping("/api/payouts")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
@Tag(name = "Payouts", description = "Manage seller payouts")
public class PayoutController {

    private final PayoutService payoutService;
    private final UserRepository userRepository;

    @Autowired
    public PayoutController(PayoutService payoutService, UserRepository userRepository) {
        this.payoutService = payoutService;
        this.userRepository = userRepository;
    }

    /**
     * Get payouts for the current user (seller).
     */
    @GetMapping
    @Operation(summary = "Get my payouts", description = "Get all payouts for the current user")
    public ResponseEntity<List<PayoutDto>> getMyPayouts() {
        User user = getCurrentUser();
        List<PayoutDto> payouts = payoutService.getPayoutsByUserId(user.getId());
        return ResponseEntity.ok(payouts);
    }

    /**
     * Get payout by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get payout by ID", description = "Get payout details by ID")
    public ResponseEntity<PayoutDto> getPayoutById(@PathVariable Long id) {
        PayoutDto payout = payoutService.getPayoutById(id);
        return ResponseEntity.ok(payout);
    }

    /**
     * Get total earnings for the current user.
     */
    @GetMapping("/earnings")
    @Operation(summary = "Get total earnings", description = "Get total earnings for the current user")
    public ResponseEntity<Map<String, Object>> getMyEarnings() {
        User user = getCurrentUser();
        BigDecimal totalEarnings = payoutService.getTotalEarnings(user.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("userName", user.getUsername());
        response.put("totalEarnings", totalEarnings);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get pending payouts (admin endpoint).
     */
    @GetMapping("/pending")
    @Operation(summary = "Get pending payouts", description = "Admin: Get all pending payouts")
    public ResponseEntity<List<PayoutDto>> getPendingPayouts() {
        List<PayoutDto> payouts = payoutService.getPendingPayouts();
        return ResponseEntity.ok(payouts);
    }

    /**
     * Get platform earnings (admin endpoint).
     */
    @GetMapping("/platform-earnings")
    @Operation(summary = "Get platform earnings", description = "Admin: Get total platform earnings")
    public ResponseEntity<Map<String, Object>> getPlatformEarnings() {
        BigDecimal totalEarnings = payoutService.getTotalPlatformEarnings();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalPlatformEarnings", totalEarnings);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get payouts for a specific user (admin endpoint).
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user payouts", description = "Admin: Get payouts for a specific user")
    public ResponseEntity<List<PayoutDto>> getUserPayouts(@PathVariable Long userId) {
        List<PayoutDto> payouts = payoutService.getPayoutsByUserId(userId);
        return ResponseEntity.ok(payouts);
    }

    /**
     * Manually trigger payout processing (admin endpoint).
     */
    @PostMapping("/process")
    @Operation(summary = "Process payouts", description = "Admin: Manually trigger payout processing")
    public ResponseEntity<Map<String, String>> processPayouts() {
        payoutService.processPendingPayouts();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Payout processing triggered successfully");
        
        return ResponseEntity.ok(response);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

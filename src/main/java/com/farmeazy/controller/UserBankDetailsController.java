package com.farmeazy.controller;

import com.farmeazy.dto.UserBankDetailsDto;
import com.farmeazy.dto.OtpResponseDto;
import com.farmeazy.dto.SensitiveActionOtpRequestDto;
import com.farmeazy.service.UserBankDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing user bank details (for seller payouts).
 */
@RestController
@RequestMapping("/api/bank-details")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
@Tag(name = "Bank Details", description = "Manage user bank details for payouts")
public class UserBankDetailsController {

    private final UserBankDetailsService bankDetailsService;

    @Autowired
    public UserBankDetailsController(UserBankDetailsService bankDetailsService) {
        this.bankDetailsService = bankDetailsService;
    }

    /**
     * Add bank details for the current user.
     */
    @PostMapping("/add")
    @Operation(summary = "Add bank details", description = "Add bank details for seller payouts")
    public ResponseEntity<UserBankDetailsDto> addBankDetails(@Valid @RequestBody UserBankDetailsDto dto) {
        UserBankDetailsDto saved = bankDetailsService.addBankDetails(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Send OTP for sensitive bank detail actions.
     */
    @PostMapping("/reauth/send")
    @Operation(summary = "Send OTP for sensitive bank action", description = "Send OTP before add/update/delete bank details")
    public ResponseEntity<OtpResponseDto> sendSensitiveActionOtp(@Valid @RequestBody SensitiveActionOtpRequestDto requestDto) {
        OtpResponseDto response = bankDetailsService.sendSensitiveActionOtp(requestDto.getAction());
        return ResponseEntity.ok(response);
    }

    /**
     * Update bank details for the current user.
     */
    @PutMapping("/update")
    @Operation(summary = "Update bank details", description = "Update existing bank details")
    public ResponseEntity<UserBankDetailsDto> updateBankDetails(@Valid @RequestBody UserBankDetailsDto dto) {
        UserBankDetailsDto updated = bankDetailsService.updateBankDetails(dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Get bank details for the current user.
     */
    @GetMapping
    @Operation(summary = "Get bank details", description = "Get current user's bank details (masked)")
    public ResponseEntity<UserBankDetailsDto> getBankDetails() {
        UserBankDetailsDto dto = bankDetailsService.getBankDetails();
        return ResponseEntity.ok(dto);
    }

    /**
     * Get bank details summary (change count, security question availability).
     */
    @GetMapping("/summary")
    @Operation(summary = "Get bank details summary", description = "Get summary info including change limit status")
    public ResponseEntity<Map<String, Object>> getBankDetailsSummary() {
        Map<String, Object> summary = bankDetailsService.getBankDetailsSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Get full unmasked bank details with security answer verification.
     */
    @PostMapping("/view-full")
    @Operation(summary = "View full bank details", description = "View full unmasked bank details after security verification")
    public ResponseEntity<UserBankDetailsDto> viewFullBankDetails(@RequestBody Map<String, String> request) {
        String securityAnswer = request.get("securityAnswer");
        UserBankDetailsDto dto = bankDetailsService.getFullBankDetails(securityAnswer);
        return ResponseEntity.ok(dto);
    }

    /**
     * Setup or update security question for bank details.
     */
    @PostMapping("/security-question")
    @Operation(summary = "Set security question", description = "Setup or update security question for viewing bank details")
    public ResponseEntity<Map<String, String>> setSecurityQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String answer = request.get("answer");
        
        if (question == null || question.isBlank() || answer == null || answer.isBlank()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Both question and answer are required");
            return ResponseEntity.badRequest().body(error);
        }
        
        bankDetailsService.setSecurityQuestion(question, answer);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Security question set successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Check if user has bank details.
     */
    @GetMapping("/check")
    @Operation(summary = "Check bank details", description = "Check if user has bank details configured")
    public ResponseEntity<Map<String, Object>> checkBankDetails() {
        Map<String, Object> response = new HashMap<>();
        response.put("hasBankDetails", bankDetailsService.hasBankDetails());
        response.put("isVerified", bankDetailsService.hasVerifiedBankDetails());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete bank details for the current user.
     */
    @DeleteMapping
    @Operation(summary = "Delete bank details", description = "Delete current user's bank details")
    public ResponseEntity<Map<String, String>> deleteBankDetails(@RequestParam String otpCode) {
        bankDetailsService.deleteBankDetails(otpCode);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Bank details deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Verify bank details (admin endpoint).
     */
    @PostMapping("/verify/{userId}")
    @Operation(summary = "Verify bank details", description = "Admin: Verify user's bank details")
    public ResponseEntity<UserBankDetailsDto> verifyBankDetails(@PathVariable Long userId) {
        UserBankDetailsDto dto = bankDetailsService.verifyBankDetails(userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get bank details by user ID (admin endpoint).
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get bank details by user ID", description = "Admin: Get bank details for a specific user")
    public ResponseEntity<UserBankDetailsDto> getBankDetailsByUserId(@PathVariable Long userId) {
        UserBankDetailsDto dto = bankDetailsService.getBankDetailsByUserId(userId);
        return ResponseEntity.ok(dto);
    }
}

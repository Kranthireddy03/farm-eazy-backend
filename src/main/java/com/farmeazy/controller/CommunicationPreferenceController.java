package com.farmeazy.controller;

import com.farmeazy.dto.CommunicationPreferenceDto;
import com.farmeazy.dto.CommunicationPreferenceResponseDto;
import com.farmeazy.service.CommunicationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * COMMUNICATION PREFERENCE CONTROLLER
 * 
 * PURPOSE: REST API for managing user notification preferences.
 * Allows users to choose between Email (free), SMS (₹0.25), or Both.
 * 
 * ENDPOINTS:
 * - GET /api/communication-preferences: Get current preferences
 * - PUT /api/communication-preferences: Update preferences
 * - GET /api/communication-preferences/pricing: Get SMS pricing info
 */
@RestController
@RequestMapping("/api/communication-preferences")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "Communication Preferences", description = "Manage notification channels (Email/SMS)")
public class CommunicationPreferenceController {

    @Autowired
    private CommunicationPreferenceService preferenceService;

    /**
     * Get user's current communication preferences
     */
    @GetMapping
    @Operation(summary = "Get communication preferences", 
               description = "Returns user's notification channel preferences with estimated costs")
    public ResponseEntity<CommunicationPreferenceResponseDto> getPreferences(Authentication auth) {
        String userEmail = auth.getName();
        CommunicationPreferenceResponseDto response = preferenceService.getPreferences(userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Update user's communication preferences
     */
    @PutMapping
    @Operation(summary = "Update communication preferences",
               description = "Update notification channel preferences. SMS requires consent.")
    public ResponseEntity<CommunicationPreferenceResponseDto> updatePreferences(
            Authentication auth,
            @Valid @RequestBody CommunicationPreferenceDto dto) {
        String userEmail = auth.getName();
        CommunicationPreferenceResponseDto response = preferenceService.updatePreferences(userEmail, dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Get SMS pricing information
     */
    @GetMapping("/pricing")
    @Operation(summary = "Get SMS pricing", description = "Returns SMS pricing and channel information")
    public ResponseEntity<SmsPricingInfo> getPricing() {
        return ResponseEntity.ok(new SmsPricingInfo());
    }

    /**
     * SMS Pricing Information Response
     */
    public record SmsPricingInfo(
            String emailCost,
            String smsCost,
            String currency,
            String[] availableChannels,
            String[] smsTemplates,
            String consentRequired
    ) {
        public SmsPricingInfo() {
            this(
                "FREE",
                "₹0.25",
                "INR",
                new String[]{"EMAIL_ONLY", "SMS_ONLY", "BOTH"},
                new String[]{
                    "FARMEAZY_LOGIN_OTP - Login OTP",
                    "FARMEAZY_PASSWORD_RESET_OTP - Password Reset",
                    "FARMEAZY_PAYMENT_SUCCESS - Payment Confirmation",
                    "FARMEAZY_PAYMENT_FAILED - Payment Failure Alert",
                    "FARMEAZY_WELCOME - Welcome Message",
                    "FARMEAZY_BOOKING_CANCELLED - Booking Cancellation",
                    "FARMEAZY_SERVICE_COMPLETED - Service Completion",
                    "FARMEAZY_IRRIGATION_REMINDER - Irrigation Alert"
                },
                "SMS requires explicit user consent before activation"
            );
        }
    }
}

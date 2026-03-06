package com.farmeazy.controller;

import com.farmeazy.dto.SmsResponseDto;
import com.farmeazy.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SMS CONTROLLER
 * 
 * PURPOSE: REST endpoints for SMS service management and testing.
 * 
 * ENDPOINTS:
 * - GET /api/sms/status - Check SMS service status (health check)
 * - POST /api/sms/test - Send test SMS (admin only, for debugging)
 */
@RestController
@RequestMapping("/api/sms")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:3000",
    "http://localhost:4200"
})
public class SmsController {
    
    private final SmsService smsService;
    
    @Autowired
    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }
    
    /**
     * Get SMS service status (health check)
     * Useful for monitoring and debugging
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        String status = smsService.getStatus();
        boolean isReady = "READY".equals(status);
        
        return ResponseEntity.ok(Map.of(
            "status", status,
            "ready", isReady,
            "message", getStatusMessage(status)
        ));
    }
    
    /**
     * Test SMS sending (for debugging/admin use)
     * Sends a test OTP to the provided phone number
     */
    @PostMapping("/test")
    public ResponseEntity<SmsResponseDto> sendTestSms(
            @RequestParam String phone,
            @RequestParam(defaultValue = "123456") String testCode) {
        
        // Check if service is configured
        if (!smsService.isConfigured()) {
            SmsResponseDto response = new SmsResponseDto();
            response.setSuccess(false);
            response.setMessage("SMS service not configured");
            response.setDisplayMessage("SMS service is not configured. Please check your MSG91 settings.");
            return ResponseEntity.ok(response);
        }
        
        // Send test OTP
        SmsResponseDto response = smsService.sendOtp(phone, testCode);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get human-readable status message
     */
    private String getStatusMessage(String status) {
        return switch (status) {
            case "READY" -> "SMS service is fully configured and ready to send messages";
            case "DISABLED" -> "SMS service is disabled. Set msg91.enabled=true to enable";
            case "NOT_CONFIGURED" -> "MSG91 auth key is not configured. Add msg91.authKey to properties";
            case "USING_PLACEHOLDER" -> "Using placeholder auth key. Add real MSG91 credentials";
            default -> "Unknown status";
        };
    }
}

package com.farmeazy.controller;

import com.farmeazy.dto.OtpRequestDto;
import com.farmeazy.dto.OtpResponseDto;
import com.farmeazy.dto.OtpVerifyDto;
import com.farmeazy.service.OtpService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OTP CONTROLLER
 * 
 * PURPOSE: REST endpoints for OTP operations
 * 
 * ENDPOINTS:
 * - POST /api/otp/send - Send OTP via Email + SMS
 * - POST /api/otp/send-detailed - Send OTP with detailed response (for popup)
 * - POST /api/otp/verify - Verify OTP
 */
@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:3000",
    "http://localhost:4200"
})
public class OtpController {
    
    private final OtpService otpService;
    
    @Autowired
    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }
    
    /**
     * Send OTP (legacy endpoint - returns simple message)
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody OtpRequestDto dto, HttpServletRequest request) {
        String message = otpService.generateAndSendOtp(
                dto,
                resolveClientIp(request),
                resolveLocation(request),
                resolveDeviceInfo(request)
        );
        return ResponseEntity.ok(Map.of("message", message));
    }
    
    /**
     * Send OTP with detailed response
     * Returns delivery channels and user-friendly message for UI popup
     */
    @PostMapping("/send-detailed")
    public ResponseEntity<OtpResponseDto> sendOtpDetailed(@Valid @RequestBody OtpRequestDto dto, HttpServletRequest request) {
        OtpResponseDto response = otpService.generateAndSendOtpWithDetails(
                dto,
                resolveClientIp(request),
                resolveLocation(request),
                resolveDeviceInfo(request)
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyDto dto) {
        boolean verified = otpService.verifyOtp(dto);
        return ResponseEntity.ok(Map.of("verified", verified, "message", "OTP verified successfully"));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return (userAgent == null || userAgent.isBlank()) ? null : userAgent;
    }

    private String resolveLocation(HttpServletRequest request) {
        String[] headers = {
                "X-User-Location",
                "CF-IPCountry",
                "CloudFront-Viewer-Country",
                "X-AppEngine-Country",
                "X-Country-Code"
        };
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"XX".equalsIgnoreCase(value.trim())) {
                return value.trim();
            }
        }

        return null;
    }
}

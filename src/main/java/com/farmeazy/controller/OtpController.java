package com.farmeazy.controller;

import com.farmeazy.dto.OtpRequestDto;
import com.farmeazy.dto.OtpVerifyDto;
import com.farmeazy.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody OtpRequestDto dto) {
        String message = otpService.generateAndSendOtp(dto);
        return ResponseEntity.ok(Map.of("message", message));
    }
    
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyDto dto) {
        boolean verified = otpService.verifyOtp(dto);
        return ResponseEntity.ok(Map.of("verified", verified, "message", "OTP verified successfully"));
    }
}

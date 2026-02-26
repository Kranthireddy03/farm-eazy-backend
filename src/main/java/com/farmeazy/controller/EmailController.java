package com.farmeazy.controller;

import com.farmeazy.dto.EmailDto;
import com.farmeazy.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * EMAIL CONTROLLER - EMAIL SENDING API ENDPOINTS
 * 
 * PURPOSE: REST API endpoints for sending emails to users.
 * Provides endpoints for various email types and notifications.
 * 
 * ENDPOINTS:
 * - POST /api/email/send - Send generic email
 * - POST /api/email/test - Send test email
 * 
 * AUTHENTICATION: All endpoints require valid JWT token.
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 * @since January 2026
 */
@RestController
@RequestMapping("/api/email")
@Tag(name = "Email", description = "Email notification endpoints")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:4200",
    "http://localhost:3000"
})
public class EmailController {

    @Autowired
    private EmailService emailService;

    /**
     * Send a generic email.
     * 
     * @param emailDto Email details (to, subject, body, isHtml)
     * @return Success response
     */
    @PostMapping("/send")
    @Operation(summary = "Send email", description = "Send a generic email to specified recipient")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid email data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "500", description = "Failed to send email")
    })
    public ResponseEntity<Map<String, String>> sendEmail(@Valid @RequestBody EmailDto emailDto) {
        if (emailDto.isHtml()) {
            emailService.sendHtmlEmail(emailDto.getTo(), emailDto.getSubject(), emailDto.getBody());
        } else {
            emailService.sendEmail(emailDto.getTo(), emailDto.getSubject(), emailDto.getBody());
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Email sent successfully");
        response.put("recipient", emailDto.getTo());
        return ResponseEntity.ok(response);
    }

    /**
     * Send a test email.
     * Useful for testing email configuration.
     * 
     * @param email Recipient email address
     * @return Success response
     */
    @PostMapping("/test")
    @Operation(summary = "Send test email", description = "Send a test email to verify email configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Test email sent successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "500", description = "Failed to send test email")
    })
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestParam String email, @RequestParam(required = false, defaultValue = "User") String name) {
        emailService.sendNotification(
            email,
            name,
            "FarmEazy Test Email",
            "This is a test email from FarmEazy. If you received this, your email configuration is working correctly!"
        );
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Test email sent successfully");
        response.put("recipient", email);
        return ResponseEntity.ok(response);
    }

    /**
     * Send welcome email to user.
     * 
     * @param email User's email address
     * @param name User's name
     * @return Success response
     */
    @PostMapping("/welcome")
    @Operation(summary = "Send welcome email", description = "Send welcome email to new user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Welcome email sent successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "500", description = "Failed to send welcome email")
    })
    public ResponseEntity<Map<String, String>> sendWelcomeEmail(
            @RequestParam String email, 
            @RequestParam String name) {
        emailService.sendWelcomeEmail(email, name);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome email sent successfully");
        response.put("recipient", email);
        return ResponseEntity.ok(response);
    }

    /**
     * Send irrigation reminder email.
     * 
     * @param email User's email address
     * @param farmName Farm name
     * @param cropName Crop name
     * @param scheduledTime Scheduled irrigation time
     * @return Success response
     */
    @PostMapping("/irrigation-reminder")
    @Operation(summary = "Send irrigation reminder", description = "Send irrigation schedule reminder to user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reminder sent successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "500", description = "Failed to send reminder")
    })
    public ResponseEntity<Map<String, String>> sendIrrigationReminder(
            @RequestParam String email,
            @RequestParam String userName,
            @RequestParam String farmName,
            @RequestParam String cropName,
            @RequestParam String scheduledTime) {
        emailService.sendIrrigationReminder(email, userName, farmName, cropName, scheduledTime);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Irrigation reminder sent successfully");
        response.put("recipient", email);
        return ResponseEntity.ok(response);
    }

    /**
     * Send harvest notification email.
     * 
     * @param email User's email address
     * @param farmName Farm name
     * @param cropName Crop name
     * @param estimatedDate Estimated harvest date
     * @return Success response
     */
    @PostMapping("/harvest-notification")
    @Operation(summary = "Send harvest notification", description = "Send crop harvest notification to user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification sent successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "500", description = "Failed to send notification")
    })
    public ResponseEntity<Map<String, String>> sendHarvestNotification(
            @RequestParam String email,
            @RequestParam String userName,
            @RequestParam String farmName,
            @RequestParam String cropName,
            @RequestParam String estimatedDate) {
        emailService.sendHarvestNotification(email, userName, farmName, cropName, estimatedDate);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Harvest notification sent successfully");
        response.put("recipient", email);
        return ResponseEntity.ok(response);
    }
}

package com.farmeazy.service;

import com.farmeazy.dto.OtpRequestDto;
import com.farmeazy.dto.OtpResponseDto;
import com.farmeazy.dto.OtpVerifyDto;
import com.farmeazy.dto.SmsResponseDto;
import com.farmeazy.entity.OtpVerification;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.OtpVerificationRepository;
import com.farmeazy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * OTP SERVICE
 * 
 * PURPOSE: Handles OTP generation, sending (email + SMS), and verification.
 * 
 * FEATURES:
 * - Sends OTP via both Email and SMS (when configured)
 * - Returns detailed response with notification status
 * - Tracks which channels were used for delivery
 */
@Service
public class OtpService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    
    private final OtpVerificationRepository otpRepository;
    private final HttpEmailService httpEmailService;
    private final SmsService smsService;
    private final UserRepository userRepository;
    private final Random random = new Random();

    @Autowired
    public OtpService(OtpVerificationRepository otpRepository, HttpEmailService httpEmailService, SmsService smsService, UserRepository userRepository) {
        this.otpRepository = otpRepository;
        this.httpEmailService = httpEmailService;
        this.smsService = smsService;
        this.userRepository = userRepository;
    }
    
    /**
     * Generate and send OTP via available channels (Email + SMS)
     * Returns detailed response with notification status for frontend popup
     */
    @Transactional
    public OtpResponseDto generateAndSendOtpWithDetails(OtpRequestDto dto) {
        OtpResponseDto response = new OtpResponseDto();
        List<String> sentVia = new ArrayList<>();
        List<String> failedVia = new ArrayList<>();
        
        // Generate 6-digit OTP
        String otpCode = String.format("%06d", random.nextInt(1000000));
        logger.info("OTP_GENERATE: email={}, purpose={}", dto.getEmail(), dto.getPurpose());

        // Create OTP verification entry
        OtpVerification otp = new OtpVerification();
        otp.setEmail(dto.getEmail());
        otp.setOtpCode(otpCode);
        otp.setPurpose(dto.getPurpose());
        otp.setVerified(false);
        otpRepository.save(otp);

        // Get user's name for personalization
        String userName = getUserName(dto);

        // 1. Send OTP via Email
        boolean emailSent = sendOtpEmail(dto.getEmail(), userName, otpCode, dto.getPurpose());
        if (emailSent) {
            sentVia.add("Email");
        } else {
            failedVia.add("Email");
        }

        // 2. Send OTP via SMS (if phone provided and SMS is configured)
        SmsResponseDto smsResponse = null;
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            if (smsService.isConfigured()) {
                smsResponse = smsService.sendOtp(dto.getPhone(), otpCode);
                if (smsResponse.isSuccess()) {
                    sentVia.add("SMS");
                } else {
                    failedVia.add("SMS");
                }
            } else {
                logger.info("OTP_SMS_SKIP: SMS service not configured, skipping SMS");
            }
        }

        // Build response
        response.setSuccess(!sentVia.isEmpty());
        response.setSentVia(sentVia);
        response.setFailedVia(failedVia.isEmpty() ? null : failedVia);
        response.setSmsResponse(smsResponse);
        
        // Build user-friendly message
        if (sentVia.isEmpty()) {
            response.setMessage("Communication failed: OTP could not be sent. Please retry.");
            response.setDisplayMessage("Failed to send OTP. Please check your contact details and try again.");
            logger.error("OTP_FAILED: No delivery channel succeeded for {}", dto.getEmail());
        } else {
            String channels = String.join(" and ", sentVia);
            response.setMessage("OTP sent successfully via " + channels + ".");
            response.setDisplayMessage("OTP sent to your " + channels.toLowerCase() + ". Please check and enter the code.");
            logger.info("OTP_SENT: Delivered via {} to {}", channels, dto.getEmail());
        }
        
        // Fallback: print to console for dev
        if (sentVia.isEmpty()) {
            System.out.println("[DEV] OTP for " + dto.getEmail() + ": " + otpCode);
        }
        
        return response;
    }
    
    /**
     * Legacy method - returns simple string (backward compatible)
     */
    @Transactional
    public String generateAndSendOtp(OtpRequestDto dto) {
        OtpResponseDto response = generateAndSendOtpWithDetails(dto);
        return response.getMessage();
    }

    /**
     * Send OTP email with retry
     */
    private boolean sendOtpEmail(String email, String userName, String otpCode, String purpose) {
        int attempts = 0;
        while (attempts < 2) {
            try {
                httpEmailService.sendOtpEmail(email, userName, otpCode, purpose);
                return true;
            } catch (Exception e) {
                attempts++;
                logger.warn("OTP_EMAIL_RETRY: Attempt {} failed for {}: {}", attempts, email, e.getMessage());
            }
        }
        return false;
    }

    /**
     * Get user name for personalization
     */
    private String getUserName(OtpRequestDto dto) {
        if ("REGISTRATION".equalsIgnoreCase(dto.getPurpose())) {
            return dto.getEmail().contains("@") 
                ? dto.getEmail().substring(0, dto.getEmail().indexOf("@"))
                : "New User";
        }
        return userRepository.findByEmail(dto.getEmail())
                            .map(com.farmeazy.entity.User::getUsername)
                            .orElse("User");
    }

    @Transactional
    public boolean verifyOtp(OtpVerifyDto dto) {
        Optional<OtpVerification> otpOpt = otpRepository.findByEmailAndOtpCodeAndPurpose(
            dto.getEmail(), 
            dto.getOtpCode(), 
            dto.getPurpose()
        );
        if (otpOpt.isEmpty()) {
            throw new UnauthorizedException("Invalid OTP code");
        }
        OtpVerification otp = otpOpt.get();
        // Check if OTP is expired
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("OTP has expired. Please request a new one");
        }
        // Check if already verified
        if (otp.isVerified()) {
            throw new UnauthorizedException("OTP has already been used");
        }
        // Mark as verified
        otp.setVerified(true);
        otp.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(otp);
        logger.info("OTP_VERIFIED: email={}, purpose={}", dto.getEmail(), dto.getPurpose());
        return true;
    }

    public boolean isOtpVerified(String email, String purpose) {
        Optional<OtpVerification> otpOpt = otpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose);
        
        if (otpOpt.isEmpty()) {
            return false;
        }
        
        OtpVerification otp = otpOpt.get();
        return otp.isVerified() && otp.getExpiresAt().isAfter(LocalDateTime.now());
    }
    
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
    
    // ========== PHONE-BASED OTP LOGIN ==========
    
    /**
     * Generate and send OTP for phone-based login
     * Uses FRMZOT sender (LOGIN_OTP template)
     * 
     * @param phone 10-digit phone number
     * @return OtpResponseDto with SMS delivery status
     */
    @Transactional
    public OtpResponseDto generateLoginOtp(String phone) {
        OtpResponseDto response = new OtpResponseDto();
        List<String> sentVia = new ArrayList<>();
        List<String> failedVia = new ArrayList<>();
        
        // Validate phone exists in system
        var userOpt = userRepository.findByPhone(phone);
        if (userOpt.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Phone number not registered");
            response.setDisplayMessage("This phone number is not registered. Please sign up first.");
            logger.warn("OTP_LOGIN_FAILED: Phone {} not found", phone);
            return response;
        }
        
        // Generate 6-digit OTP
        String otpCode = String.format("%06d", random.nextInt(1000000));
        logger.info("OTP_LOGIN_GENERATE: phone={}", maskPhone(phone));
        
        // Create OTP verification entry (phone-based)
        OtpVerification otp = new OtpVerification();
        otp.setPhone(phone);
        otp.setEmail(userOpt.get().getEmail()); // Also store email for reference
        otp.setOtpCode(otpCode);
        otp.setPurpose("LOGIN");
        otp.setVerified(false);
        otpRepository.save(otp);
        
        // Send OTP via SMS
        if (smsService.isConfigured()) {
            SmsResponseDto smsResponse = smsService.sendOtp(phone, otpCode, "10");
            if (smsResponse.isSuccess()) {
                sentVia.add("SMS");
                response.setSmsResponse(smsResponse);
            } else {
                failedVia.add("SMS");
                logger.warn("OTP_LOGIN_SMS_FAILED: {}", smsResponse.getMessage());
            }
        } else {
            logger.warn("OTP_LOGIN_SMS_SKIP: SMS service not configured");
            failedVia.add("SMS (not configured)");
        }
        
        // Build response
        response.setSuccess(!sentVia.isEmpty());
        response.setSentVia(sentVia);
        response.setFailedVia(failedVia.isEmpty() ? null : failedVia);
        
        if (sentVia.isEmpty()) {
            response.setMessage("Failed to send OTP. Please try again.");
            response.setDisplayMessage("Could not send OTP to your phone. Please try again.");
            
            // Dev fallback - print to console
            System.out.println("[DEV] LOGIN OTP for " + phone + ": " + otpCode);
        } else {
            response.setMessage("OTP sent to your registered mobile number.");
            response.setDisplayMessage("OTP sent to " + maskPhone(phone) + ". Valid for 10 minutes.");
        }
        
        return response;
    }
    
    /**
     * Verify phone-based login OTP
     * 
     * @param phone 10-digit phone number
     * @param otpCode 6-digit OTP
     * @return true if OTP is valid
     * @throws UnauthorizedException if OTP is invalid/expired
     */
    @Transactional
    public boolean verifyLoginOtp(String phone, String otpCode) {
        Optional<OtpVerification> otpOpt = otpRepository.findByPhoneAndOtpCodeAndPurpose(
            phone, otpCode, "LOGIN"
        );
        
        if (otpOpt.isEmpty()) {
            logger.warn("OTP_LOGIN_INVALID: phone={}", maskPhone(phone));
            throw new UnauthorizedException("Invalid OTP code");
        }
        
        OtpVerification otp = otpOpt.get();
        
        // Check expiry
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("OTP_LOGIN_EXPIRED: phone={}", maskPhone(phone));
            throw new UnauthorizedException("OTP has expired. Please request a new one.");
        }
        
        // Check if already used
        if (otp.isVerified()) {
            logger.warn("OTP_LOGIN_USED: phone={}", maskPhone(phone));
            throw new UnauthorizedException("OTP has already been used. Please request a new one.");
        }
        
        // Mark as verified
        otp.setVerified(true);
        otp.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(otp);
        
        logger.info("OTP_LOGIN_VERIFIED: phone={}", maskPhone(phone));
        return true;
    }
    
    /**
     * Mask phone for logging
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
    }
}

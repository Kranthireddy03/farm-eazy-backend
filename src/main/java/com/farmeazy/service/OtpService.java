package com.farmeazy.service;

import com.farmeazy.dto.OtpRequestDto;
import com.farmeazy.dto.OtpResponseDto;
import com.farmeazy.dto.OtpVerifyDto;
import com.farmeazy.dto.SmsResponseDto;
import com.farmeazy.entity.OtpVerification;
import com.farmeazy.exception.DuplicateResourceException;
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
        return generateAndSendOtpWithDetails(dto, null, null, null);
    }

    @Transactional
    public OtpResponseDto generateAndSendOtpWithDetails(OtpRequestDto dto, String ipAddress, String location, String deviceInfo) {
        OtpResponseDto response = new OtpResponseDto();
        List<String> sentVia = new ArrayList<>();
        List<String> failedVia = new ArrayList<>();

        String targetPhone = dto.getPhone();
        if (targetPhone == null || targetPhone.isBlank()) {
            targetPhone = userRepository.findByEmail(dto.getEmail())
                    .map(com.farmeazy.entity.User::getPhone)
                    .orElse(null);
        }

        // Registration OTP should only be generated for new accounts
        if ("REGISTRATION".equalsIgnoreCase(dto.getPurpose())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new DuplicateResourceException("Email already registered. Please login instead.");
            }
            if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
                boolean phoneTaken = userRepository.findAllByPhone(dto.getPhone()).stream()
                        .anyMatch(user -> !user.getEmail().equalsIgnoreCase(dto.getEmail()));
                if (phoneTaken) {
                    throw new DuplicateResourceException("Phone number already registered. Please use a different phone number.");
                }
            }
        }

        expirePreviousOtpsByEmail(dto.getEmail(), dto.getPurpose());
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            expirePreviousOtpsByPhone(dto.getPhone(), dto.getPurpose());
        }
        
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
        boolean emailSent = sendOtpEmail(dto.getEmail(), userName, otpCode, dto.getPurpose(), ipAddress, location, deviceInfo);
        if (emailSent) {
            sentVia.add("Email");
        } else {
            failedVia.add("Email");
        }

        // 2. Send OTP via SMS (if phone provided and SMS is configured)
        SmsResponseDto smsResponse = null;
        if (targetPhone != null && !targetPhone.isBlank()) {
            if (smsService.isConfigured()) {
                try {
                    logger.info("OTP_SMS_ATTEMPT: email={}, phone={}", dto.getEmail(), maskPhone(targetPhone));
                    if ("BANK_VERIFICATION".equalsIgnoreCase(dto.getPurpose())
                            || "REFUND_DETAILS_UPDATE".equalsIgnoreCase(dto.getPurpose())
                            || "REFUND_DETAILS_DELETE".equalsIgnoreCase(dto.getPurpose())) {
                        logger.info("OTP_SMS_FLOW: Using BANK_DETAILS_OTP template for email={}", dto.getEmail());
                        smsResponse = smsService.sendBankDetailsOtp(
                                targetPhone,
                                resolveBankOtpAction(dto.getPurpose()),
                                otpCode,
                                "10");
                    } else {
                        smsResponse = smsService.sendOtp(targetPhone, otpCode, "10");
                    }
                    if (smsResponse != null && smsResponse.isSuccess()) {
                        sentVia.add("SMS");
                        logger.info("OTP_SMS_SENT: email={}, phone={}", dto.getEmail(), maskPhone(targetPhone));
                    } else {
                        failedVia.add("SMS");
                        logger.warn("OTP_SMS_FAILED: email={}, phone={}, reason={}",
                                dto.getEmail(),
                                maskPhone(targetPhone),
                                smsResponse != null ? smsResponse.getMessage() : "No response from SMS service");
                    }
                } catch (Exception e) {
                    logger.warn("OTP_SMS_EXCEPTION: Failed to send SMS OTP for {}: {}", maskPhone(targetPhone), e.getMessage());
                    failedVia.add("SMS");
                }
            } else {
                logger.info("OTP_SMS_SKIP: SMS service not configured, skipping SMS for email={}", dto.getEmail());
                failedVia.add("SMS (not configured)");
            }
        } else {
            logger.info("OTP_SMS_SKIP: No phone available in request/profile for email={}", dto.getEmail());
            failedVia.add("SMS (no phone)");
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

            if (sentVia.contains("Email") && failedVia.contains("SMS")) {
                response.setDisplayMessage("OTP sent via email. SMS delivery failed; please retry if you need SMS.");
            } else if (sentVia.contains("SMS") && failedVia.contains("Email")) {
                response.setDisplayMessage("OTP sent via SMS. Email delivery failed; please check your email if you need it.");
            } else {
                response.setDisplayMessage("OTP sent to your " + channels.toLowerCase() + ". Please check and enter the code.");
            }

            logger.info("OTP_SENT: Delivered via {} to {}", channels, dto.getEmail());
        }
        
        // Fallback: print to console for dev
        if (sentVia.isEmpty()) {
            System.out.println("[DEV] OTP for " + dto.getEmail() + ": " + otpCode);
        }
        
        return response;
    }

    private void expirePreviousOtpsByEmail(String email, String purpose) {
        if (email == null || email.isBlank() || purpose == null || purpose.isBlank()) {
            return;
        }
        java.util.List<OtpVerification> previousOtps = otpRepository.findByEmailAndPurposeAndVerifiedFalse(email, purpose);
        if (!previousOtps.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            previousOtps.forEach(existing -> existing.setExpiresAt(now));
            otpRepository.saveAll(previousOtps);
        }
    }

    private void expirePreviousOtpsByPhone(String phone, String purpose) {
        if (phone == null || phone.isBlank() || purpose == null || purpose.isBlank()) {
            return;
        }
        java.util.List<OtpVerification> previousOtps = otpRepository.findByPhoneAndPurposeAndVerifiedFalse(phone, purpose);
        if (!previousOtps.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            previousOtps.forEach(existing -> existing.setExpiresAt(now));
            otpRepository.saveAll(previousOtps);
        }
    }
    
    /**
     * Legacy method - returns simple string (backward compatible)
     */
    @Transactional
    public String generateAndSendOtp(OtpRequestDto dto) {
        return generateAndSendOtp(dto, null, null, null);
    }

    @Transactional
    public String generateAndSendOtp(OtpRequestDto dto, String ipAddress, String location, String deviceInfo) {
        OtpResponseDto response = generateAndSendOtpWithDetails(dto, ipAddress, location, deviceInfo);
        return response.getMessage();
    }

    /**
     * Send OTP email with retry
     */
    private boolean sendOtpEmail(String email, String userName, String otpCode, String purpose) {
        return sendOtpEmail(email, userName, otpCode, purpose, null, null, null);
    }

    private boolean sendOtpEmail(String email, String userName, String otpCode, String purpose,
                                 String ipAddress, String location, String deviceInfo) {
        int attempts = 0;
        while (attempts < 2) {
            try {
                httpEmailService.sendOtpEmail(email, userName, otpCode, purpose, ipAddress, location, deviceInfo);
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

    private String resolveBankOtpAction(String purpose) {
        if (purpose == null) {
            return "addition";
        }

        if ("REFUND_DETAILS_DELETE".equalsIgnoreCase(purpose)) {
            return "deletion";
        }
        if ("REFUND_DETAILS_UPDATE".equalsIgnoreCase(purpose)) {
            return "update";
        }
        return "addition";
    }

    @Transactional
    public boolean verifyOtp(OtpVerifyDto dto) {
        Optional<OtpVerification> otpOpt = otpRepository.findTopByEmailAndOtpCodeAndPurposeOrderByCreatedAtDesc(
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

        // Send welcome email and SMS after registration OTP verification
        if ("REGISTRATION".equalsIgnoreCase(dto.getPurpose())) {
            // Try to fetch user by email
            userRepository.findByEmail(dto.getEmail()).ifPresent(user -> {
                try {
                    httpEmailService.sendWelcomeEmailAsync(user.getEmail(), user.getUsername());
                } catch (Exception e) {
                    logger.warn("Failed to send welcome email: {}", e.getMessage());
                }
                try {
                    SmsResponseDto smsResponse = smsService.sendWelcome(user.getPhone(), user.getUsername());
                    if (!smsResponse.isSuccess()) {
                        logger.warn("Welcome SMS failed for {}: {} (display: {})", user.getPhone(), smsResponse.getMessage(), smsResponse.getDisplayMessage());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to send welcome SMS: {}", e.getMessage());
                }
            });
            // Also handle phone-based registration (if phone is present)
            if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
                userRepository.findByPhone(dto.getPhone()).ifPresent(user -> {
                    try {
                        httpEmailService.sendWelcomeEmailAsync(user.getEmail(), user.getUsername());
                    } catch (Exception e) {
                        logger.warn("Failed to send welcome email (phone-based): {}", e.getMessage());
                    }
                    try {
                        SmsResponseDto smsResponse = smsService.sendWelcome(user.getPhone(), user.getUsername());
                        if (!smsResponse.isSuccess()) {
                            logger.warn("Welcome SMS failed (phone-based) for {}: {} (display: {})", user.getPhone(), smsResponse.getMessage(), smsResponse.getDisplayMessage());
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to send welcome SMS (phone-based): {}", e.getMessage());
                    }
                });
            }
        }
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
        return generateLoginOtp(phone, null, null, null);
    }

    @Transactional
    public OtpResponseDto generateLoginOtp(String phone, String ipAddress, String location, String deviceInfo) {
        OtpResponseDto response = new OtpResponseDto();
        List<String> sentVia = new ArrayList<>();
        List<String> failedVia = new ArrayList<>();
        
        // Validate phone exists in system
        var userOpt = resolveUserByPhone(phone);
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
        String userEmail = userOpt.get().getEmail();
        String userName = userOpt.get().getUsername() != null && !userOpt.get().getUsername().isBlank()
            ? userOpt.get().getUsername()
            : "User";
        
        expirePreviousOtpsByPhone(phone, "LOGIN");
        expirePreviousOtpsByEmail(userEmail, "LOGIN");

        // Create OTP verification entry (phone-based)
        OtpVerification otp = new OtpVerification();
        otp.setPhone(phone);
        otp.setEmail(userEmail); // Also store email for reference
        otp.setOtpCode(otpCode);
        otp.setPurpose("LOGIN");
        otp.setVerified(false);
        otpRepository.save(otp);

        // Send OTP via Email to the account associated with this phone number.
        boolean emailSent = false;
        if (userEmail != null && !userEmail.isBlank()) {
            emailSent = sendOtpEmail(userEmail, userName, otpCode, "LOGIN", ipAddress, location, deviceInfo);
            if (emailSent) {
                sentVia.add("Email");
            } else {
                failedVia.add("Email");
                logger.warn("OTP_LOGIN_EMAIL_FAILED: phone={}, email={}", maskPhone(phone), userEmail);
            }
        } else {
            failedVia.add("Email (missing)");
            logger.warn("OTP_LOGIN_EMAIL_SKIP: Missing email for phone={}", maskPhone(phone));
        }
        
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
            response.setDisplayMessage("Could not send OTP via email or SMS. Please try again.");
            
            // Dev fallback - print to console
            System.out.println("[DEV] LOGIN OTP for " + phone + ": " + otpCode);
        } else {
            String channels = String.join(" and ", sentVia);
            response.setMessage("OTP sent via " + channels + ".");
            response.setDisplayMessage("OTP sent via " + channels.toLowerCase() + ". Valid for 10 minutes.");
        }
        
        return response;
    }

    private Optional<com.farmeazy.entity.User> resolveUserByPhone(String phone) {
        List<com.farmeazy.entity.User> users = userRepository.findAllByPhone(phone);
        if (users == null || users.isEmpty()) {
            return Optional.empty();
        }

        return users.stream()
                .filter(user -> user.getActive() == null || user.getActive())
                .findFirst()
                .or(() -> users.stream().findFirst());
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
        Optional<OtpVerification> otpOpt = otpRepository.findTopByPhoneAndOtpCodeAndPurposeOrderByCreatedAtDesc(
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
     * Check whether a LOGIN OTP was already verified and is still within validity window.
     * This is used by multi-step flows (verify -> change password) that re-submit the same code.
     */
    @Transactional(readOnly = true)
    public boolean isVerifiedLoginOtpStillValid(String phone, String otpCode) {
        Optional<OtpVerification> otpOpt = otpRepository.findTopByPhoneAndOtpCodeAndPurposeOrderByCreatedAtDesc(
            phone, otpCode, "LOGIN"
        );
        if (otpOpt.isEmpty()) {
            return false;
        }
        OtpVerification otp = otpOpt.get();
        return otp.isVerified() && otp.getExpiresAt() != null && otp.getExpiresAt().isAfter(LocalDateTime.now());
    }
    
    /**
     * Mask phone for logging
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
    }
}

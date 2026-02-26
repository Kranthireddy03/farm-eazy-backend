package com.farmeazy.service;

import com.farmeazy.dto.OtpRequestDto;
import com.farmeazy.dto.OtpVerifyDto;
import com.farmeazy.entity.OtpVerification;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.OtpVerificationRepository;
import com.farmeazy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {
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
    
    @Transactional
    public String generateAndSendOtp(OtpRequestDto dto) {
        // Generate 6-digit OTP
        String otpCode = String.format("%06d", random.nextInt(1000000));

        // Create OTP verification entry
        OtpVerification otp = new OtpVerification();
        otp.setEmail(dto.getEmail());
        otp.setOtpCode(otpCode);
        otp.setPurpose(dto.getPurpose());
        otp.setVerified(false);

        otpRepository.save(otp);

        // Get user's name for email personalization
        String userName = userRepository.findByEmail(dto.getEmail())
                                        .map(com.farmeazy.entity.User::getFullName)
                                        .orElse("User");

        // Send OTP via email
        boolean emailSent = false;
        int emailAttempts = 0;
        while (!emailSent && emailAttempts < 2) {
            try {
                httpEmailService.sendOtpEmail(dto.getEmail(), userName, otpCode, dto.getPurpose());
                emailSent = true;
            } catch (Exception e) {
                emailAttempts++;
                System.err.println("Failed to send OTP email (attempt " + emailAttempts + "): " + e.getMessage());
            }
        }

        // SMS temporarily disabled - uncomment when SMS service is configured
        boolean smsSent = false;
        /*
        // Send OTP via SMS
        int smsAttempts = 0;
        while (!smsSent && smsAttempts < 2) {
            try {
                smsService.sendSms(dto.getPhone(), "Your FarmEazy OTP is: " + otpCode);
                smsSent = true;
            } catch (Exception e) {
                smsAttempts++;
                System.err.println("Failed to send OTP SMS (attempt " + smsAttempts + "): " + e.getMessage());
            }
        }
        */

        // Fallback: print to console for dev
        if (!emailSent && !smsSent) {
            System.out.println("OTP for " + dto.getEmail() + "/" + dto.getPhone() + ": " + otpCode);
            return "Communication failed: OTP could not be sent via Email or SMS. Please retry.";
        }
        String result = "OTP sent successfully";
        if (emailSent && smsSent) {
            result += " via Email and SMS.";
        } else if (emailSent) {
            result += " via Email (SMS failed, please retry).";
        } else if (smsSent) {
            result += " via SMS (Email failed, please retry).";
        }
        return result;
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
}

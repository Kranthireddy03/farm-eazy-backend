package com.farmeazy.service;

import com.farmeazy.dto.UserBankDetailsDto;
import com.farmeazy.dto.OtpRequestDto;
import com.farmeazy.dto.OtpResponseDto;
import com.farmeazy.dto.OtpVerifyDto;
import com.farmeazy.dto.BankVerificationRequestDto;
import com.farmeazy.entity.User;
import com.farmeazy.entity.UserBankDetails;
import com.farmeazy.exception.DuplicateResourceException;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.UserBankDetailsRepository;
import com.farmeazy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;

/**
 * Service for managing user bank details for seller payouts.
 * 
 * Security Features:
 * 1. Change Limit: Users can only change bank details 3 times
 * 2. Security Question: Required to view full bank details
 * 3. OTP Verification: Required before adding/updating/deleting bank details
 * 4. Masked Display: Account numbers are always masked by default
 */
@Service
public class UserBankDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserBankDetailsService.class);
    private final UserBankDetailsRepository bankDetailsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;
    private final HttpEmailService emailService;
    private final OtpService otpService;
    private final SecurityAuditService securityAuditService;
    private final BankVerificationService bankVerificationService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String BANK_ADD_PURPOSE = "BANK_DETAILS_ADD";
    private static final String BANK_UPDATE_PURPOSE = "BANK_DETAILS_UPDATE";
    private static final String BANK_DELETE_PURPOSE = "BANK_DETAILS_DELETE";

    @Autowired
    public UserBankDetailsService(UserBankDetailsRepository bankDetailsRepository, 
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  SmsService smsService,
                                  HttpEmailService emailService,
                                  OtpService otpService,
                                  SecurityAuditService securityAuditService,
                                  BankVerificationService bankVerificationService) {
        this.bankDetailsRepository = bankDetailsRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsService = smsService;
        this.emailService = emailService;
        this.otpService = otpService;
        this.securityAuditService = securityAuditService;
        this.bankVerificationService = bankVerificationService;
    }

    /**
     * Add bank details for the current user.
     * Sends SMS and email notification after successful addition.
     */
    @Transactional
    public UserBankDetailsDto addBankDetails(UserBankDetailsDto dto) {
        User user = getCurrentUser();

        try {
            validateOtpForAction(user, dto.getOtpCode(), "ADD");
        } catch (Exception ex) {
            securityAuditService.logBankAction(user,
                    "Bank details add blocked: OTP validation failed",
                    false,
                    "reason=" + ex.getMessage());
            throw ex;
        }

        // Check if bank details already exist
        if (bankDetailsRepository.existsByUserId(user.getId())) {
            securityAuditService.logBankAction(user,
                    "Bank details add blocked: record already exists",
                    false,
                    null);
            throw new DuplicateResourceException("Bank details already exist for this user. Use update instead.");
        }

        UserBankDetails bankDetails = new UserBankDetails();
        bankDetails.setUser(user);
        bankDetails.setAccountHolderName(dto.getAccountHolderName());
        bankDetails.setAccountNumber(dto.getAccountNumber());
        bankDetails.setIfscCode(dto.getIfscCode().toUpperCase());
        bankDetails.setBankName(dto.getBankName());
        bankDetails.setBranchName(dto.getBranchName());
        bankDetails.setUpiId(dto.getUpiId());
        bankDetails.setIsVerified(false); // Bank details need verification
        bankDetails.setIsPrimary(true);
        bankDetails.setChangeCount(0); // First addition doesn't count as a change

        // Set security question if provided
        if (dto.getSecurityQuestion() != null && dto.getSecurityAnswer() != null) {
            bankDetails.setSecurityQuestion(dto.getSecurityQuestion());
            bankDetails.setSecurityAnswer(passwordEncoder.encode(dto.getSecurityAnswer().toLowerCase().trim()));
        }

        UserBankDetails saved = bankDetailsRepository.save(bankDetails);
        securityAuditService.logBankAction(user,
            "Bank details added after OTP re-auth",
            true,
            "bank=" + dto.getBankName());

        // Send notification SMS
        try {
            if (user.getPhone() != null) {
                smsService.sendBankUpdateAlert(user.getPhone(), "added");
            }
        } catch (Exception e) {
            System.out.println("[BankDetails] SMS notification failed: " + e.getMessage());
        }

        // Send notification email (using SUPPORT sender for security-related notifications)
        try {
            emailService.sendNotificationEmail(
                user.getEmail(),
                user.getUsername(),
                "Bank Details Added - FarmEazy",
                "Your bank details have been successfully added to your FarmEazy account. " +
                "Bank: " + dto.getBankName() + ", Account ending in ****" + 
                dto.getAccountNumber().substring(dto.getAccountNumber().length() - 4) + ". " +
                "If you did not perform this action, please contact support immediately at support@farm-eazy.com.",
                EmailType.SUPPORT
            );
        } catch (Exception e) {
            System.out.println("[BankDetails] Email notification failed: " + e.getMessage());
        }

        // AUTO-TRIGGER PENNY DROP VERIFICATION
        try {
            logger.info("AUTO_TRIGGER_PENNY_DROP: Initiating auto penny drop for userId={}, bank={}", user.getId(), dto.getBankName());
            BankVerificationRequestDto verificationDto = new BankVerificationRequestDto();
            verificationDto.setVerificationType("BANK_ACCOUNT");
            verificationDto.setAccountHolderName(dto.getAccountHolderName());
            verificationDto.setAccountNumber(dto.getAccountNumber());
            verificationDto.setIfscCode(dto.getIfscCode());
            verificationDto.setBankName(dto.getBankName());
            verificationDto.setBranchName(dto.getBranchName());
            bankVerificationService.initiateVerification(user.getId(), verificationDto);
            logger.info("AUTO_TRIGGER_SUCCESS: Penny drop initiated for userId={}", user.getId());
        } catch (Exception e) {
            logger.warn("AUTO_TRIGGER_FAILED: Penny drop auto-trigger failed for userId={}. Error: {}", user.getId(), e.getMessage());
        }

        return toDto(saved);
    }

    /**
     * Update bank details for the current user.
     * Enforces 3-time change limit and sends notifications.
     */
    @Transactional
    public UserBankDetailsDto updateBankDetails(UserBankDetailsDto dto) {
        User user = getCurrentUser();

        try {
            validateOtpForAction(user, dto.getOtpCode(), "UPDATE");
        } catch (Exception ex) {
            securityAuditService.logBankAction(user,
                    "Bank details update blocked: OTP validation failed",
                    false,
                    "reason=" + ex.getMessage());
            throw ex;
        }

        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank details not found"));

        // Check change limit (max 3 changes)
        if (bankDetails.hasReachedChangeLimit()) {
            securityAuditService.logBankAction(user,
                    "Bank details update blocked: change limit reached",
                    false,
                    "changeCount=" + bankDetails.getChangeCount());
            throw new UnauthorizedException("You have reached the maximum limit of 3 bank detail changes. Please contact support to update your bank details.");
        }

        bankDetails.setAccountHolderName(dto.getAccountHolderName());
        bankDetails.setAccountNumber(dto.getAccountNumber());
        bankDetails.setIfscCode(dto.getIfscCode().toUpperCase());
        bankDetails.setBankName(dto.getBankName());
        bankDetails.setBranchName(dto.getBranchName());
        bankDetails.setUpiId(dto.getUpiId());
        // Reset verification when details are updated
        bankDetails.setIsVerified(false);
        bankDetails.setVerificationDate(null);
        // Increment change count
        bankDetails.incrementChangeCount();

        UserBankDetails saved = bankDetailsRepository.save(bankDetails);
        securityAuditService.logBankAction(user,
            "Bank details updated after OTP re-auth",
            true,
            "remainingChanges=" + saved.getRemainingChanges());

        // Send notification SMS
        try {
            if (user.getPhone() != null) {
                smsService.sendBankUpdateAlert(user.getPhone(), "updated");
            }
        } catch (Exception e) {
            System.out.println("[BankDetails] SMS notification failed: " + e.getMessage());
        }

        // Send notification email (using SUPPORT sender for security-related notifications)
        try {
            emailService.sendNotificationEmail(
                user.getEmail(),
                user.getUsername(),
                "Bank Details Updated - FarmEazy",
                "Your bank details have been successfully updated on FarmEazy. " +
                "Bank: " + dto.getBankName() + ", Account ending in ****" + 
                dto.getAccountNumber().substring(dto.getAccountNumber().length() - 4) + ". " +
                "You have " + saved.getRemainingChanges() + " change(s) remaining. " +
                "If you did not perform this action, please contact support immediately at support@farm-eazy.com.",
                EmailType.SUPPORT
            );
        } catch (Exception e) {
            System.out.println("[BankDetails] Email notification failed: " + e.getMessage());
        }

        return toDto(saved);
    }

    /**
     * Get bank details for the current user.
     * Returns MASKED data - use getFullBankDetails with security verification for unmasked.
     */
    public UserBankDetailsDto getBankDetails() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank details not found. Please add your bank details."));

        return toDto(bankDetails);
    }

    /**
     * Get bank details summary info (change count, security question availability).
     */
    public java.util.Map<String, Object> getBankDetailsSummary() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        java.util.Map<String, Object> summary = new java.util.HashMap<>();
        
        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(user.getId()).orElse(null);
        
        if (bankDetails != null) {
            summary.put("hasBankDetails", true);
            summary.put("changeCount", bankDetails.getChangeCount());
            summary.put("remainingChanges", bankDetails.getRemainingChanges());
            summary.put("canChange", !bankDetails.hasReachedChangeLimit());
            summary.put("hasSecurityQuestion", bankDetails.getSecurityQuestion() != null);
            summary.put("securityQuestion", bankDetails.getSecurityQuestion());
            summary.put("maskedAccountNumber", bankDetails.getMaskedAccountNumber());
            summary.put("bankName", bankDetails.getBankName());
        } else {
            summary.put("hasBankDetails", false);
            summary.put("changeCount", 0);
            summary.put("remainingChanges", 3);
            summary.put("canChange", true);
            summary.put("hasSecurityQuestion", false);
        }
        
        return summary;
    }

    /**
     * Get full unmasked bank details after security question verification.
     */
    public UserBankDetailsDto getFullBankDetails(String securityAnswer) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank details not found"));

        // Verify security answer if set
        if (bankDetails.getSecurityAnswer() != null) {
            if (securityAnswer == null || securityAnswer.isBlank()) {
                throw new UnauthorizedException("Security answer is required to view full bank details");
            }
            if (!passwordEncoder.matches(securityAnswer.toLowerCase().trim(), bankDetails.getSecurityAnswer())) {
                throw new UnauthorizedException("Incorrect security answer");
            }
        }

        // Return FULL (unmasked) details
        return toDtoFull(bankDetails);
    }

    /**
     * Setup or update security question for bank details.
     */
    @Transactional
    public void setSecurityQuestion(String question, String answer) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank details not found. Add bank details first."));

        bankDetails.setSecurityQuestion(question);
        bankDetails.setSecurityAnswer(passwordEncoder.encode(answer.toLowerCase().trim()));
        bankDetailsRepository.save(bankDetails);
    }

    /**
     * Get bank details for a specific user (admin function).
     */
    public UserBankDetailsDto getBankDetailsByUserId(Long userId) {
        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank details not found for user ID: " + userId));
        return toDto(bankDetails);
    }

    /**
     * Check if user has bank details.
     */
    public boolean hasBankDetails() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return bankDetailsRepository.existsByUserId(user.getId());
    }

    /**
     * Check if user has verified bank details.
     */
    public boolean hasVerifiedBankDetails() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return bankDetailsRepository.findByUserIdAndIsVerifiedTrue(user.getId()).isPresent();
    }

    /**
     * Delete bank details for the current user.
     * Sends notifications after deletion.
     */
    @Transactional
    public void deleteBankDetails(String otpCode) {
        User user = getCurrentUser();

        try {
            validateOtpForAction(user, otpCode, "DELETE");
        } catch (Exception ex) {
            securityAuditService.logBankAction(user,
                    "Bank details delete blocked: OTP validation failed",
                    false,
                    "reason=" + ex.getMessage());
            throw ex;
        }

        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank details not found"));

        String bankName = bankDetails.getBankName();
        String maskedAccount = bankDetails.getMaskedAccountNumber();

        bankDetailsRepository.delete(bankDetails);
        securityAuditService.logBankAction(user,
            "Bank details deleted after OTP re-auth",
            true,
            "bank=" + bankName + ",account=" + maskedAccount);

        // Send notification SMS
        try {
            if (user.getPhone() != null) {
                smsService.sendBankUpdateAlert(user.getPhone(), "deleted");
            }
        } catch (Exception e) {
            System.out.println("[BankDetails] SMS notification failed: " + e.getMessage());
        }

        // Send notification email (using SUPPORT sender for security-related notifications)
        try {
            emailService.sendNotificationEmail(
                user.getEmail(),
                user.getUsername(),
                "Bank Details Removed - FarmEazy",
                "Your bank details have been removed from your FarmEazy account. " +
                "Bank: " + bankName + ", Account ending in " + maskedAccount + ". " +
                "If you did not perform this action, please contact support immediately at support@farm-eazy.com.",
                EmailType.SUPPORT
            );
        } catch (Exception e) {
            System.out.println("[BankDetails] Email notification failed: " + e.getMessage());
        }
    }

    /**
     * Verify bank details (admin/system function).
     */
    @Transactional
    public UserBankDetailsDto verifyBankDetails(Long userId) {
        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank details not found for user ID: " + userId));

        bankDetails.setIsVerified(true);
        bankDetails.setVerificationDate(java.time.LocalDateTime.now());

        UserBankDetails saved = bankDetailsRepository.save(bankDetails);
        return toDto(saved);
    }

    public OtpResponseDto sendSensitiveActionOtp() {
        User user = getCurrentUser();

        OtpRequestDto request = new OtpRequestDto();
        request.setEmail(user.getEmail());
        request.setPhone(user.getPhone());
        request.setPurpose(BANK_UPDATE_PURPOSE);

        try {
            OtpResponseDto response = otpService.generateAndSendOtpWithDetails(request);
            securityAuditService.logBankAction(user,
                    "Sensitive bank OTP sent",
                    true,
                    "purpose=" + BANK_UPDATE_PURPOSE);
            return response;
        } catch (Exception ex) {
            securityAuditService.logBankAction(user,
                    "Sensitive bank OTP send failed",
                    false,
                    "reason=" + ex.getMessage());
            throw ex;
        }
    }

    public OtpResponseDto sendSensitiveActionOtp(String action) {
        User user = getCurrentUser();
        String purpose = resolvePurpose(action);

        OtpRequestDto request = new OtpRequestDto();
        request.setEmail(user.getEmail());
        request.setPhone(user.getPhone());
        request.setPurpose(purpose);

        try {
            OtpResponseDto response = otpService.generateAndSendOtpWithDetails(request);
            securityAuditService.logBankAction(user,
                    "Sensitive bank OTP sent for action: " + action,
                    true,
                    "purpose=" + purpose);
            return response;
        } catch (Exception ex) {
            securityAuditService.logBankAction(user,
                    "Sensitive bank OTP send failed for action: " + action,
                    false,
                    "purpose=" + purpose + ";reason=" + ex.getMessage());
            throw ex;
        }
    }

    private void validateOtpForAction(User user, String otpCode, String action) {
        if (otpCode == null || otpCode.isBlank()) {
            throw new IllegalArgumentException("OTP code is required for this action");
        }

        OtpVerifyDto verifyDto = new OtpVerifyDto();
        verifyDto.setEmail(user.getEmail());
        verifyDto.setPhone(user.getPhone());
        verifyDto.setOtpCode(otpCode);
        verifyDto.setPurpose(resolvePurpose(action));
        otpService.verifyOtp(verifyDto);
    }

    private String resolvePurpose(String action) {
        if ("ADD".equalsIgnoreCase(action)) {
            return BANK_ADD_PURPOSE;
        }
        if ("DELETE".equalsIgnoreCase(action)) {
            return BANK_DELETE_PURPOSE;
        }
        if ("UPDATE".equalsIgnoreCase(action)) {
            return BANK_UPDATE_PURPOSE;
        }
        throw new IllegalArgumentException("Unsupported sensitive action");
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Convert entity to DTO (masking sensitive data).
     */
    private UserBankDetailsDto toDto(UserBankDetails entity) {
        UserBankDetailsDto dto = new UserBankDetailsDto();
        dto.setId(entity.getId());
        dto.setAccountHolderName(entity.getAccountHolderName());
        // Mask account number for security
        dto.setMaskedAccountNumber(entity.getMaskedAccountNumber());
        dto.setAccountNumber(entity.getMaskedAccountNumber()); // Return masked version
        dto.setIfscCode(entity.getIfscCode());
        dto.setBankName(entity.getBankName());
        dto.setBranchName(entity.getBranchName());
        dto.setUpiId(entity.getUpiId());
        dto.setIsVerified(entity.getIsVerified());
        dto.setIsPrimary(entity.getIsPrimary());
        // Security info
        dto.setChangeCount(entity.getChangeCount());
        dto.setRemainingChanges(entity.getRemainingChanges());
        dto.setCanChange(!entity.hasReachedChangeLimit());
        dto.setHasSecurityQuestion(entity.getSecurityQuestion() != null);
        dto.setSecurityQuestion(entity.getSecurityQuestion());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(formatter));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().format(formatter));
        }
        return dto;
    }

    /**
     * Convert entity to DTO with FULL unmasked account number.
     * Only use after security verification.
     */
    private UserBankDetailsDto toDtoFull(UserBankDetails entity) {
        UserBankDetailsDto dto = toDto(entity);
        // Override with full account number
        dto.setAccountNumber(entity.getAccountNumber());
        return dto;
    }
}

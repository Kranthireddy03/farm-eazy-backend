package com.farmeazy.service;

import com.farmeazy.dto.UserBankDetailsDto;
import com.farmeazy.entity.User;
import com.farmeazy.entity.UserBankDetails;
import com.farmeazy.exception.DuplicateResourceException;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.UserBankDetailsRepository;
import com.farmeazy.repository.UserRepository;
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

    private final UserBankDetailsRepository bankDetailsRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;
    private final HttpEmailService emailService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    public UserBankDetailsService(UserBankDetailsRepository bankDetailsRepository, 
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  SmsService smsService,
                                  HttpEmailService emailService) {
        this.bankDetailsRepository = bankDetailsRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsService = smsService;
        this.emailService = emailService;
    }

    /**
     * Add bank details for the current user.
     * Sends SMS and email notification after successful addition.
     */
    @Transactional
    public UserBankDetailsDto addBankDetails(UserBankDetailsDto dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if bank details already exist
        if (bankDetailsRepository.existsByUserId(user.getId())) {
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

        return toDto(saved);
    }

    /**
     * Update bank details for the current user.
     * Enforces 3-time change limit and sends notifications.
     */
    @Transactional
    public UserBankDetailsDto updateBankDetails(UserBankDetailsDto dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank details not found"));

        // Check change limit (max 3 changes)
        if (bankDetails.hasReachedChangeLimit()) {
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
    public void deleteBankDetails() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bank details not found"));

        String bankName = bankDetails.getBankName();
        String maskedAccount = bankDetails.getMaskedAccountNumber();

        bankDetailsRepository.delete(bankDetails);

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

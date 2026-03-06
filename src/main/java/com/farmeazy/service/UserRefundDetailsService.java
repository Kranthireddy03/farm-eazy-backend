package com.farmeazy.service;

import com.farmeazy.dto.UserRefundDetailsDto;
import com.farmeazy.entity.User;
import com.farmeazy.entity.UserRefundDetails;
import com.farmeazy.entity.UserRefundDetails.RefundMethod;
import com.farmeazy.exception.DuplicateResourceException;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.UserRefundDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing user refund details (bank/UPI info for receiving refunds).
 * 
 * @author FarmEazy Development Team
 */
@Service
@Transactional
public class UserRefundDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserRefundDetailsService.class);

    @Autowired
    private UserRefundDetailsRepository refundDetailsRepository;

    /**
     * Save or update refund details for a user.
     * One record per user - updates if exists, creates if not.
     */
    public UserRefundDetailsDto saveOrUpdate(User user, UserRefundDetailsDto dto) {
        log.info("Saving refund details for user: {}", user.getEmail());

        // Validate bank details if preferred method is BANK
        if ("BANK".equalsIgnoreCase(dto.getPreferredMethod())) {
            if (dto.getAccountNumber() == null || dto.getAccountNumber().isEmpty()) {
                throw new IllegalArgumentException("Account number is required for bank refund");
            }
            if (!dto.getAccountNumber().equals(dto.getConfirmAccountNumber())) {
                throw new IllegalArgumentException("Account numbers do not match");
            }
            if (dto.getIfscCode() == null || dto.getIfscCode().isEmpty()) {
                throw new IllegalArgumentException("IFSC code is required for bank refund");
            }
        } else {
            // UPI method
            if (dto.getUpiId() == null || dto.getUpiId().isEmpty()) {
                throw new IllegalArgumentException("UPI ID is required for UPI refund");
            }
        }

        Optional<UserRefundDetails> existing = refundDetailsRepository.findByUser(user);

        UserRefundDetails entity;
        if (existing.isPresent()) {
            entity = existing.get();
            log.info("Updating existing refund details for user: {}", user.getEmail());
        } else {
            entity = new UserRefundDetails();
            entity.setUser(user);
            log.info("Creating new refund details for user: {}", user.getEmail());
        }

        // Update fields
        entity.setAccountHolderName(dto.getAccountHolderName());
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setConfirmAccountNumber(dto.getConfirmAccountNumber());
        entity.setIfscCode(dto.getIfscCode());
        entity.setBankName(dto.getBankName());
        entity.setBranchName(dto.getBranchName());
        entity.setUpiId(dto.getUpiId());
        entity.setPreferredMethod(RefundMethod.valueOf(dto.getPreferredMethod().toUpperCase()));

        // Reset verification if details changed
        if (existing.isPresent()) {
            entity.setIsVerified(false);
            entity.setVerificationDate(null);
        }

        UserRefundDetails saved = refundDetailsRepository.save(entity);
        return convertToDto(saved);
    }

    /**
     * Get refund details for a user.
     */
    public Optional<UserRefundDetailsDto> getByUser(User user) {
        return refundDetailsRepository.findByUser(user)
                .map(this::convertToDto);
    }

    /**
     * Check if user has valid refund details.
     */
    public boolean hasValidRefundDetails(User user) {
        Optional<UserRefundDetails> details = refundDetailsRepository.findByUser(user);
        if (details.isEmpty()) {
            return false;
        }
        return details.get().hasValidRefundDetails();
    }

    /**
     * Get refund details entity for a user.
     */
    public Optional<UserRefundDetails> getRefundDetailsEntity(User user) {
        return refundDetailsRepository.findByUser(user);
    }

    /**
     * Verify refund details (admin action).
     */
    public UserRefundDetailsDto verifyRefundDetails(Long id) {
        UserRefundDetails entity = refundDetailsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund details not found"));

        entity.setIsVerified(true);
        entity.setVerificationDate(LocalDateTime.now());

        UserRefundDetails saved = refundDetailsRepository.save(entity);
        log.info("Verified refund details ID: {}", id);
        return convertToDto(saved);
    }

    /**
     * Delete refund details for a user.
     */
    public void deleteByUser(User user) {
        refundDetailsRepository.deleteByUser(user);
        log.info("Deleted refund details for user: {}", user.getEmail());
    }

    /**
     * Convert entity to DTO.
     */
    private UserRefundDetailsDto convertToDto(UserRefundDetails entity) {
        UserRefundDetailsDto dto = new UserRefundDetailsDto();
        dto.setId(entity.getId());
        dto.setAccountHolderName(entity.getAccountHolderName());
        dto.setAccountNumber(maskAccountNumber(entity.getAccountNumber()));
        dto.setConfirmAccountNumber(null); // Don't return this
        dto.setIfscCode(entity.getIfscCode());
        dto.setBankName(entity.getBankName());
        dto.setBranchName(entity.getBranchName());
        dto.setUpiId(entity.getUpiId());
        dto.setPreferredMethod(entity.getPreferredMethod().name());
        dto.setIsVerified(entity.getIsVerified());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        return dto;
    }

    /**
     * Mask account number for security (show last 4 digits only).
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        int visibleDigits = 4;
        String masked = "*".repeat(accountNumber.length() - visibleDigits);
        return masked + accountNumber.substring(accountNumber.length() - visibleDigits);
    }
}

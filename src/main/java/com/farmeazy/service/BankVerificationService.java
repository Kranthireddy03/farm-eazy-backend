package com.farmeazy.service;

import com.farmeazy.dto.BankVerificationRequestDto;
import com.farmeazy.dto.BankVerificationResponseDto;
import com.farmeazy.entity.*;
import com.farmeazy.entity.BankVerificationRequest.*;
import com.farmeazy.entity.CommunicationLog.CommunicationPurpose;
import com.farmeazy.entity.CommunicationLog.CommunicationType;
import com.farmeazy.entity.CommunicationLog.CommunicationStatus;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * BANK VERIFICATION SERVICE
 * 
 * PURPOSE: Verifies bank accounts by sending 1 rupee transfers.
 * Implements professional rate limiting to prevent abuse.
 * 
 * KEY FEATURES:
 * - 1 rupee transfer for verification
 * - Daily limit (default 3 attempts per day)
 * - Monthly limit (default 10 attempts per month)
 * - Total lifetime limit (default 50 attempts)
 * - Masked data storage for security
 * - Email/SMS notifications
 * 
 * RATE LIMITING:
 * - Prevents abuse of verification system
 * - Users cannot change bank details unlimited times
 * - Professional handling with clear messaging
 * 
 * SECURITY:
 * - Account numbers stored as SHA-256 hashes
 * - Only masked versions shown in logs/UI
 * - Full audit trail maintained
 */
@Service
public class BankVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(BankVerificationService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    @Autowired
    private BankVerificationRequestRepository verificationRepository;

    @Autowired
    private BankVerificationLimitRepository limitRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunicationLogRepository communicationLogRepository;

    @Autowired
    private SequenceGeneratorService sequenceService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Value("${farmeazy.bank-verification.daily-limit:3}")
    private int defaultDailyLimit;

    @Value("${farmeazy.bank-verification.monthly-limit:10}")
    private int defaultMonthlyLimit;

    @Value("${farmeazy.bank-verification.total-limit:50}")
    private int defaultTotalLimit;

    @Value("${farmeazy.bank-verification.amount:1.00}")
    private BigDecimal verificationAmount;

    // ========== VERIFICATION REQUEST ==========

    /**
     * Initiates a bank verification request.
     * Checks rate limits before proceeding.
     * 
     * @return Response with status and remaining attempts
     */
    @Transactional
    public BankVerificationResponseDto initiateVerification(Long userId, BankVerificationRequestDto dto) {
        logger.info("BANK_VERIFY_INIT: userId={}, type={}", userId, dto.getVerificationType());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        // Check rate limits
        BankVerificationLimit limit = getOrCreateLimit(user);
        
        if (!limit.canVerify()) {
            return buildLimitExceededResponse(limit);
        }
        
        // Generate unique verification ID
        String verificationNumber = sequenceService.getNextBankVerificationId();
        
        // Create verification request
        BankVerificationRequest request = new BankVerificationRequest();
        request.setVerificationNumber(verificationNumber);
        request.setUser(user);
        request.setVerificationType(VerificationType.valueOf(dto.getVerificationType()));
        request.setTransferAmount(verificationAmount);
        request.setStatus(VerificationStatus.INITIATED);
        request.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24 hour validity
        
        // Store masked and hashed data
        if (VerificationType.BANK_ACCOUNT.name().equals(dto.getVerificationType())) {
            request.setAccountHolderName(dto.getAccountHolderName());
            request.setAccountNumberMasked(maskAccountNumber(dto.getAccountNumber()));
            request.setAccountNumberHash(hashValue(dto.getAccountNumber()));
            request.setIfscCode(dto.getIfscCode());
            request.setBankName(dto.getBankName());
            request.setBranchName(dto.getBranchName());
        } else {
            request.setUpiIdMasked(maskUpiId(dto.getUpiId()));
            request.setUpiIdHash(hashValue(dto.getUpiId()));
        }
        
        request = verificationRepository.save(request);
        
        // Update limits
        limit.incrementUsage(verificationAmount);
        limitRepository.save(limit);
        
        logger.info("BANK_VERIFY_CREATED: verificationNumber={}, userId={}, type={}",
                verificationNumber, userId, dto.getVerificationType());
        auditLogger.info("BANK_VERIFICATION_INITIATED: verificationNumber={}, userId={}, accountMasked={}",
                verificationNumber, userId, 
                request.getAccountNumberMasked() != null ? request.getAccountNumberMasked() : request.getUpiIdMasked());
        
        // Initiate transfer
        initiateTransfer(request, dto);
        
        return buildSuccessResponse(request, limit);
    }

    /**
     * Checks if user can verify (rate limit check only, doesn't consume attempt).
     */
    @Transactional
    public BankVerificationResponseDto checkVerificationEligibility(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        BankVerificationLimit limit = limitRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultLimit(user));
        
        BankVerificationResponseDto response = new BankVerificationResponseDto();
        response.setCanVerify(limit.canVerify());
        response.setRemainingToday(limit.getRemainingToday());
        response.setRemainingThisMonth(limit.getRemainingThisMonth());
        response.setRemainingTotal(limit.getRemainingTotal());
        response.setTotalSpent(limit.getTotalAmountSpent());
        response.setBlocked(limit.getIsBlocked());
        response.setBlockedReason(limit.getBlockedReason());
        
        if (limit.getIsBlocked()) {
            response.setMessage("Bank verification is blocked. " + limit.getBlockedReason());
        } else if (!limit.canVerify()) {
            response.setMessage(buildLimitMessage(limit));
        } else {
            response.setMessage("You can proceed with bank verification. ₹" + verificationAmount + 
                    " will be transferred for verification.");
        }
        
        return response;
    }

    /**
     * Confirms verification after user receives the amount.
     */
    @Transactional
    public BankVerificationResponseDto confirmVerification(String verificationNumber) {
        BankVerificationRequest request = verificationRepository.findByVerificationNumber(verificationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found: " + verificationNumber));
        
        if (request.getStatus() == VerificationStatus.TRANSFER_SUCCESS) {
            request.setStatus(VerificationStatus.VERIFIED);
            request.setVerifiedAt(LocalDateTime.now());
            verificationRepository.save(request);
            
            logger.info("BANK_VERIFY_CONFIRMED: verificationNumber={}", verificationNumber);
            auditLogger.info("BANK_VERIFICATION_COMPLETED: verificationNumber={}, userId={}",
                    verificationNumber, request.getUser().getId());
            
            // Send success notification
            sendVerificationResultNotification(request, true);
            
            BankVerificationResponseDto response = new BankVerificationResponseDto();
            response.setSuccess(true);
            response.setVerificationNumber(verificationNumber);
            response.setStatus(VerificationStatus.VERIFIED.name());
            response.setMessage("Bank account verified successfully!");
            return response;
        } else {
            BankVerificationResponseDto response = new BankVerificationResponseDto();
            response.setSuccess(false);
            response.setVerificationNumber(verificationNumber);
            response.setStatus(request.getStatus().name());
            response.setMessage("Cannot confirm verification. Current status: " + request.getStatus());
            return response;
        }
    }

    // ========== TRANSFER PROCESSING ==========

    /**
     * Initiates the 1 rupee transfer via payment gateway.
     * This would integrate with Razorpay Payouts or similar.
     */
    private void initiateTransfer(BankVerificationRequest request, BankVerificationRequestDto dto) {
        try {
            request.setStatus(VerificationStatus.TRANSFER_PENDING);
            request.setTransferAttemptedAt(LocalDateTime.now());
            verificationRepository.save(request);
            
            // TODO: Integrate with Razorpay Payouts API
            // For now, simulate transfer
            // In production, this would call:
            // - Razorpay Payouts API for bank transfers
            // - UPI transfer API for UPI verification
            
            // Simulate successful transfer (replace with actual API call)
            simulateTransfer(request);
            
        } catch (Exception e) {
            logger.error("BANK_VERIFY_TRANSFER_FAILED: verificationNumber={}, error={}",
                    request.getVerificationNumber(), e.getMessage());
            
            request.setStatus(VerificationStatus.TRANSFER_FAILED);
            request.setTransferErrorMessage(e.getMessage());
            verificationRepository.save(request);
            
            sendVerificationResultNotification(request, false);
        }
    }

    /**
     * Simulates transfer for development.
     * Replace with actual Razorpay integration.
     */
    private void simulateTransfer(BankVerificationRequest request) {
        // Simulate processing delay
        request.setTransferReferenceId("TXN" + System.currentTimeMillis());
        request.setTransferGateway("Razorpay");
        request.setTransferStatus("processed");
        request.setStatus(VerificationStatus.TRANSFER_SUCCESS);
        request.setTransferCompletedAt(LocalDateTime.now());
        
        verificationRepository.save(request);
        
        logger.info("BANK_VERIFY_TRANSFER_SUCCESS: verificationNumber={}, refId={}",
                request.getVerificationNumber(), request.getTransferReferenceId());
        
        // Notify user
        sendVerificationResultNotification(request, true);
    }

    // ========== RATE LIMIT MANAGEMENT ==========

    /**
     * Gets or creates verification limit for user.
     */
    private BankVerificationLimit getOrCreateLimit(User user) {
        return limitRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultLimit(user));
    }

    /**
     * Creates default limit for new user.
     */
    private BankVerificationLimit createDefaultLimit(User user) {
        BankVerificationLimit limit = new BankVerificationLimit();
        limit.setUser(user);
        limit.setDailyLimit(defaultDailyLimit);
        limit.setMonthlyLimit(defaultMonthlyLimit);
        limit.setTotalLimit(defaultTotalLimit);
        return limitRepository.save(limit);
    }

    /**
     * Resets daily limits (called by scheduler).
     */
    @Transactional
    public int resetDailyLimits() {
        int count = limitRepository.resetDailyCounters(LocalDate.now());
        logger.info("BANK_VERIFY_DAILY_RESET: count={}", count);
        return count;
    }

    /**
     * Resets monthly limits (called by scheduler).
     */
    @Transactional
    public int resetMonthlyLimits() {
        int count = limitRepository.resetMonthlyCounters(LocalDate.now());
        logger.info("BANK_VERIFY_MONTHLY_RESET: count={}", count);
        return count;
    }

    /**
     * Blocks a user from verification.
     */
    @Transactional
    public void blockUser(Long userId, String reason) {
        BankVerificationLimit limit = limitRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User limit not found"));
        
        limit.setIsBlocked(true);
        limit.setBlockedReason(reason);
        limit.setBlockedAt(LocalDateTime.now());
        limitRepository.save(limit);
        
        logger.warn("BANK_VERIFY_USER_BLOCKED: userId={}, reason={}", userId, reason);
        auditLogger.warn("BANK_VERIFICATION_BLOCKED: userId={}, reason={}", userId, reason);
    }

    // ========== QUERY METHODS ==========

    /**
     * Get user's verification history.
     */
    @Transactional(readOnly = true)
    public Page<BankVerificationRequest> getUserVerifications(Long userId, Pageable pageable) {
        return verificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get verification by number.
     */
    @Transactional(readOnly = true)
    public BankVerificationRequest getByVerificationNumber(String verificationNumber) {
        return verificationRepository.findByVerificationNumber(verificationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Verification not found: " + verificationNumber));
    }

    /**
     * Get pending verifications for batch processing.
     */
    @Transactional(readOnly = true)
    public List<BankVerificationRequest> getPendingVerifications() {
        return verificationRepository.findPendingTransfers();
    }

    // ========== NOTIFICATIONS ==========

    /**
     * Sends verification result notification to user.
     */
    private void sendVerificationResultNotification(BankVerificationRequest request, boolean success) {
        try {
            String subject = success 
                    ? "Bank Verification Successful - " + request.getVerificationNumber()
                    : "Bank Verification Failed - " + request.getVerificationNumber();
            
            String body = buildVerificationEmailBody(request, success);
            
            emailService.sendEmail(request.getUser().getEmail(), subject, body);
            
            request.setUserNotified(true);
            request.setNotificationType(NotificationType.EMAIL);
            request.setNotificationSentAt(LocalDateTime.now());
            verificationRepository.save(request);
            
            logCommunication(request, success);
            
            // Send Bank Verification SMS
            try {
                String userPhone = request.getUser().getPhone();
                if (userPhone != null && !userPhone.isBlank()) {
                    smsService.sendBankDetailsAlert(
                        userPhone,
                        success ? "VERIFICATION_SUCCESS" : "VERIFICATION_FAILED"
                    );
                }
            } catch (Exception smsEx) {
                logger.error("BANK_VERIFY_SMS_FAILED: verificationNumber={}, error={}", 
                        request.getVerificationNumber(), smsEx.getMessage());
            }

        } catch (Exception e) {
            logger.error("BANK_VERIFY_NOTIFICATION_FAILED: verificationNumber={}, error={}", 
                    request.getVerificationNumber(), e.getMessage());
        }
    }

    private String buildVerificationEmailBody(BankVerificationRequest request, boolean success) {
        if (success) {
            return String.format(
                    "Dear %s,\n\n" +
                    "Your bank verification was successful!\n\n" +
                    "Verification ID: %s\n" +
                    "Amount Transferred: ₹%s\n" +
                    "Reference ID: %s\n" +
                    "Account: %s\n\n" +
                    "Please log in to confirm receipt of the amount to complete verification.\n\n" +
                    "Best regards,\nFarmEazy Team",
                    request.getUser().getUsername(),
                    request.getVerificationNumber(),
                    request.getTransferAmount(),
                    request.getTransferReferenceId(),
                    request.getAccountNumberMasked() != null 
                            ? request.getAccountNumberMasked() 
                            : request.getUpiIdMasked()
            );
        } else {
            return String.format(
                    "Dear %s,\n\n" +
                    "We were unable to verify your bank account.\n\n" +
                    "Verification ID: %s\n" +
                    "Reason: %s\n\n" +
                    "Please check your bank details and try again, or contact support.\n\n" +
                    "Best regards,\nFarmEazy Team",
                    request.getUser().getUsername(),
                    request.getVerificationNumber(),
                    request.getTransferErrorMessage() != null 
                            ? request.getTransferErrorMessage() 
                            : "Transfer failed"
            );
        }
    }

    // ========== UTILITY METHODS ==========

    /**
     * Masks account number for safe display/logging.
     * Example: 1234567890 → ****7890
     */
    public String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Masks UPI ID for safe display/logging.
     * Example: john@okicici → jo***@***
     */
    public String maskUpiId(String upiId) {
        if (upiId == null || !upiId.contains("@")) {
            return "****";
        }
        String[] parts = upiId.split("@");
        String masked = parts[0].length() > 2 
                ? parts[0].substring(0, 2) + "***" 
                : "***";
        return masked + "@***";
    }

    /**
     * Creates SHA-256 hash of value for secure storage.
     */
    private String hashValue(String value) {
        if (value == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Builds response for successful verification initiation.
     */
    private BankVerificationResponseDto buildSuccessResponse(BankVerificationRequest request, 
            BankVerificationLimit limit) {
        
        BankVerificationResponseDto response = new BankVerificationResponseDto();
        response.setSuccess(true);
        response.setVerificationNumber(request.getVerificationNumber());
        response.setStatus(request.getStatus().name());
        response.setTransferAmount(request.getTransferAmount());
        response.setCanVerify(true);
        response.setRemainingToday(limit.getRemainingToday());
        response.setRemainingThisMonth(limit.getRemainingThisMonth());
        response.setRemainingTotal(limit.getRemainingTotal());
        response.setMessage("Verification initiated. ₹" + verificationAmount + 
                " will be transferred to your account within 24 hours.");
        return response;
    }

    /**
     * Builds response for exceeded limits.
     */
    private BankVerificationResponseDto buildLimitExceededResponse(BankVerificationLimit limit) {
        BankVerificationResponseDto response = new BankVerificationResponseDto();
        response.setSuccess(false);
        response.setCanVerify(false);
        response.setRemainingToday(limit.getRemainingToday());
        response.setRemainingThisMonth(limit.getRemainingThisMonth());
        response.setRemainingTotal(limit.getRemainingTotal());
        response.setTotalSpent(limit.getTotalAmountSpent());
        response.setBlocked(limit.getIsBlocked());
        response.setMessage(buildLimitMessage(limit));
        
        logger.warn("BANK_VERIFY_LIMIT_EXCEEDED: userId={}, dailyUsed={}, monthlyUsed={}, totalUsed={}",
                limit.getUser().getId(), limit.getUsedToday(), limit.getUsedThisMonth(), limit.getTotalUsed());
        
        return response;
    }

    /**
     * Builds user-friendly limit message.
     */
    private String buildLimitMessage(BankVerificationLimit limit) {
        if (limit.getIsBlocked()) {
            return "Bank verification is blocked. " + limit.getBlockedReason();
        }
        if (limit.getRemainingToday() <= 0) {
            return "Daily verification limit reached. Please try again tomorrow.";
        }
        if (limit.getRemainingThisMonth() <= 0) {
            return "Monthly verification limit reached. Please try again next month.";
        }
        if (limit.getRemainingTotal() <= 0) {
            return "Total verification limit reached. Please contact support for assistance.";
        }
        return "Verification limit exceeded.";
    }

    /**
     * Logs communication for audit.
     */
    private void logCommunication(BankVerificationRequest request, boolean success) {
        CommunicationLog log = new CommunicationLog();
        log.setCommunicationType(CommunicationType.EMAIL);
        log.setPurpose(CommunicationPurpose.BANK_VERIFICATION);
        log.setRecipientUser(request.getUser());
        log.setRecipientEmail(request.getUser().getEmail());
        log.setSubject("Bank Verification " + (success ? "Successful" : "Failed"));
        log.setContentSummary("Verification: " + request.getVerificationNumber());
        log.setReferenceType("BANK_VERIFICATION");
        log.setReferenceId(request.getId());
        log.setStatus(CommunicationStatus.SENT);
        log.setProvider("EmailService");
        log.setSentAt(LocalDateTime.now());
        
        communicationLogRepository.save(log);
    }
}

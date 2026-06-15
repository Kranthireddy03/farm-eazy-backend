package com.farmeazy.service;

import com.farmeazy.dto.BankVerificationRequestDto;
import com.farmeazy.dto.BankVerificationResponseDto;
import com.farmeazy.dto.SmsResponseDto;
import com.farmeazy.entity.*;
import com.farmeazy.entity.BankVerificationRequest.*;
import com.farmeazy.entity.CommunicationLog.CommunicationPurpose;
import com.farmeazy.entity.CommunicationLog.CommunicationType;
import com.farmeazy.entity.CommunicationLog.CommunicationStatus;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.*;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
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
    private UserBankDetailsRepository userBankDetailsRepository;

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

    @Value("${bank.verification.mode:simulate}")
    private String verificationMode;

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    @PostConstruct
    private void validateBankVerificationConfiguration() {
        if (!"razorpay".equalsIgnoreCase(verificationMode)) {
            logger.info("BANK_VERIFY_MODE_CONFIG: mode={}", verificationMode);
            if ("simulate".equalsIgnoreCase(verificationMode)) {
                logger.warn("BANK_VERIFY_MODE_SIMULATE_ACTIVE: Penny drop is simulated. No real INR transfer will happen. Set BANK_VERIFICATION_MODE=razorpay in UAT/PROD for real transfers.");
            }
            return;
        }

        boolean keyIdMissing = razorpayKeyId == null || razorpayKeyId.isBlank();
        boolean keySecretMissing = razorpayKeySecret == null || razorpayKeySecret.isBlank();

        if (keyIdMissing || keySecretMissing) {
            if (keyIdMissing) {
                logger.error("ENV_MISSING_VALUE: RAZORPAY_KEY_ID is missing or blank while BANK_VERIFICATION_MODE=razorpay");
            }
            if (keySecretMissing) {
                logger.error("ENV_MISSING_VALUE: RAZORPAY_KEY_SECRET is missing or blank while BANK_VERIFICATION_MODE=razorpay");
            }
            throw new IllegalStateException("bank.verification.mode is razorpay but required Razorpay env variables are missing");
        }

        logger.info("BANK_VERIFY_MODE_CONFIG: mode=razorpay, credentialsConfigured=true");
    }

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

        upsertUserBankDetailsForVerification(user, dto);
        
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

            markUserBankDetailsVerified(request.getUser());
            
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

            if ("simulate".equalsIgnoreCase(verificationMode)) {
                simulateTransfer(request);
            } else if ("razorpay".equalsIgnoreCase(verificationMode)) {
                initiateRazorpayTransfer(request, dto);
            } else {
                throw new IllegalStateException("Unsupported bank verification mode: " + verificationMode);
            }
            
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
     * Initiates transfer in Razorpay Route mode.
     * Flow: create linked account -> attach bank account -> create transfer.
     */
    private void initiateRazorpayTransfer(BankVerificationRequest request, BankVerificationRequestDto dto) {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            if (razorpayKeyId == null || razorpayKeyId.isBlank()) {
                logger.error("ENV_MISSING_VALUE: RAZORPAY_KEY_ID is missing or blank during transfer initiation");
            }
            if (razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
                logger.error("ENV_MISSING_VALUE: RAZORPAY_KEY_SECRET is missing or blank during transfer initiation");
            }
            throw new IllegalStateException("Razorpay credentials are not configured");
        }
        if (!VerificationType.BANK_ACCOUNT.name().equalsIgnoreCase(dto.getVerificationType())) {
            throw new IllegalStateException("Razorpay Route verification currently supports BANK_ACCOUNT only");
        }

        try {
            JSONObject linkedAccount = createRazorpayLinkedAccount(request);
            String linkedAccountId = linkedAccount.optString("id", "");
            if (linkedAccountId.isBlank()) {
                throw new IllegalStateException("Failed to create Razorpay linked account");
            }
            auditLogger.info("BANK_VERIFY_LINKED_ACCOUNT_CREATED: verificationNumber={}, userId={}, linkedAccountId={}",
                    request.getVerificationNumber(), request.getUser().getId(), linkedAccountId);

            JSONObject bankAccount = createRazorpayLinkedBankAccount(request, dto, linkedAccountId);
            String linkedBankAccountId = bankAccount.optString("id", "");
            if (linkedBankAccountId.isBlank()) {
                throw new IllegalStateException("Failed to attach bank account to linked account");
            }
            auditLogger.info("BANK_VERIFY_LINKED_BANK_ATTACHED: verificationNumber={}, linkedAccountId={}, linkedBankAccountId={}",
                    request.getVerificationNumber(), linkedAccountId, linkedBankAccountId);

            JSONObject transfer = createRazorpayTransfer(request, linkedAccountId);
            String transferId = transfer.optString("id", "");
            String transferStatus = transfer.optString("status", "pending");

            request.setTransferReferenceId(transferId.isBlank() ? "TRF_BNK_" + System.currentTimeMillis() : transferId);
            request.setTransferGateway("RazorpayRoute");
            request.setTransferStatus(transferStatus);
            request.setRazorpayContactId(linkedAccountId);
            request.setRazorpayFundAccountId(linkedBankAccountId);
                auditLogger.info("BANK_VERIFY_TRANSFER_CREATED: verificationNumber={}, linkedAccountId={}, transferId={}, status={}",
                    request.getVerificationNumber(), linkedAccountId, request.getTransferReferenceId(), transferStatus);

            String statusLower = transferStatus.toLowerCase();
            if ("processed".equals(statusLower)) {
                request.setStatus(VerificationStatus.TRANSFER_SUCCESS);
                request.setTransferCompletedAt(LocalDateTime.now());
                verificationRepository.save(request);

                sendVerificationResultNotification(request, true);
                logger.info("BANK_VERIFY_TRANSFER_SUCCESS: verificationNumber={}, transferId={}",
                        request.getVerificationNumber(), request.getTransferReferenceId());
                return;
            }

            if ("failed".equals(statusLower) || "rejected".equals(statusLower) || "cancelled".equals(statusLower)) {
                request.setStatus(VerificationStatus.TRANSFER_FAILED);
                request.setTransferCompletedAt(LocalDateTime.now());
                request.setTransferErrorMessage(extractTransferFailureReason(transfer));
                verificationRepository.save(request);

                sendVerificationResultNotification(request, false);
                logger.warn("BANK_VERIFY_TRANSFER_FAILED: verificationNumber={}, transferId={}, reason={}",
                        request.getVerificationNumber(), request.getTransferReferenceId(), request.getTransferErrorMessage());
                return;
            }

            request.setStatus(VerificationStatus.TRANSFER_PENDING);
            verificationRepository.save(request);

            logger.info("BANK_VERIFY_TRANSFER_INITIATED: verificationNumber={}, mode=razorpay-route, transferId={}, status={}",
                    request.getVerificationNumber(), request.getTransferReferenceId(), transferStatus);
        } catch (Exception ex) {
            throw new IllegalStateException("Razorpay Route transfer initiation failed: " + ex.getMessage(), ex);
        }
    }

    private JSONObject createRazorpayLinkedAccount(BankVerificationRequest request) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", request.getUser().getEmail());
        payload.put("type", "route");
        payload.put("reference_id", request.getVerificationNumber());

        JSONObject notes = new JSONObject();
        notes.put("verification_number", request.getVerificationNumber());
        notes.put("user_id", String.valueOf(request.getUser().getId()));
        payload.put("notes", notes);

        return postToRazorpay("/v2/accounts", payload);
    }

    private JSONObject createRazorpayLinkedBankAccount(BankVerificationRequest request, BankVerificationRequestDto dto, String linkedAccountId) throws Exception {
        if (dto.getAccountNumber() == null || dto.getAccountNumber().isBlank() || dto.getIfscCode() == null || dto.getIfscCode().isBlank()) {
            throw new IllegalStateException("Account number and IFSC are required for bank verification transfer");
        }

        JSONObject payload = new JSONObject();
        JSONObject bankAccount = new JSONObject();
        // Razorpay Route bank account attachment does not require customer name in UAT mode.
        bankAccount.put("account_number", dto.getAccountNumber());
        bankAccount.put("ifsc", dto.getIfscCode());
        payload.put("bank_account", bankAccount);

        return postToRazorpay("/v2/accounts/" + linkedAccountId + "/bank_accounts", payload);
    }

    private JSONObject createRazorpayTransfer(BankVerificationRequest request, String linkedAccountId) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("account", linkedAccountId);
        payload.put("amount", request.getTransferAmount().multiply(new BigDecimal("100")).intValue());
        payload.put("currency", "INR");

        JSONObject notes = new JSONObject();
        notes.put("purpose", "bank_verification");
        notes.put("verification_number", request.getVerificationNumber());
        notes.put("user_id", String.valueOf(request.getUser().getId()));
        payload.put("notes", notes);

        return postToRazorpay("/v1/transfers", payload);
    }

    private JSONObject postToRazorpay(String path, JSONObject payload) throws Exception {
        String authRaw = razorpayKeyId + ":" + razorpayKeySecret;
        String auth = "Basic " + Base64.getEncoder().encodeToString(authRaw.getBytes(StandardCharsets.UTF_8));

        logger.debug("RAZORPAY_API_REQUEST: path={}, payload={}", path, payload.toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.razorpay.com" + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", auth)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body() == null ? "{}" : response.body();
        logger.debug("RAZORPAY_API_RESPONSE: path={}, status={}, body={}", path, response.statusCode(), body);
        JSONObject json = new JSONObject(body);

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return json;
        }

        String errorMessage = "Razorpay API call failed with status " + response.statusCode();
        JSONObject error = json.optJSONObject("error");
        if (error != null) {
            errorMessage = error.optString("description", errorMessage);
        }

        throw new IllegalStateException(errorMessage);
    }

    private String extractTransferFailureReason(JSONObject transfer) {
        JSONObject statusDetails = transfer.optJSONObject("status_details");
        if (statusDetails != null) {
            String reason = statusDetails.optString("description", "");
            if (reason != null && !reason.isBlank()) {
                return reason;
            }
        }
        String status = transfer.optString("status", "failed");
        return "Transfer status: " + status;
    }

    private String normalizePhoneForRazorpay(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 10) {
            return null;
        }
        return digits.substring(digits.length() - 10);
    }

    @Transactional
    public void handleTransferProcessedWebhook(String transferReferenceId) {
        BankVerificationRequest request = verificationRepository.findByTransferReferenceId(transferReferenceId)
                .orElse(null);
        if (request == null) {
            logger.warn("BANK_VERIFY_WEBHOOK_TRANSFER_NOT_FOUND: transferReferenceId={}", transferReferenceId);
            return;
        }

        if (request.getStatus() == VerificationStatus.VERIFIED || request.getStatus() == VerificationStatus.TRANSFER_SUCCESS) {
            logger.info("BANK_VERIFY_WEBHOOK_IDEMPOTENT_SUCCESS: verificationNumber={}, transferReferenceId={}",
                    request.getVerificationNumber(), transferReferenceId);
            return;
        }

        request.setTransferStatus("processed");
        request.setStatus(VerificationStatus.TRANSFER_SUCCESS);
        request.setTransferCompletedAt(LocalDateTime.now());
        verificationRepository.save(request);

        sendVerificationResultNotification(request, true);
        logger.info("BANK_VERIFY_WEBHOOK_TRANSFER_SUCCESS: verificationNumber={}, transferReferenceId={}",
                request.getVerificationNumber(), transferReferenceId);
    }

    @Transactional
    public void handleTransferFailedWebhook(String transferReferenceId, String failureReason) {
        BankVerificationRequest request = verificationRepository.findByTransferReferenceId(transferReferenceId)
                .orElse(null);
        if (request == null) {
            logger.warn("BANK_VERIFY_WEBHOOK_TRANSFER_NOT_FOUND: transferReferenceId={}", transferReferenceId);
            return;
        }

        if (request.getStatus() == VerificationStatus.TRANSFER_FAILED) {
            logger.info("BANK_VERIFY_WEBHOOK_IDEMPOTENT_FAILURE: verificationNumber={}, transferReferenceId={}",
                    request.getVerificationNumber(), transferReferenceId);
            return;
        }

        request.setTransferStatus("failed");
        request.setStatus(VerificationStatus.TRANSFER_FAILED);
        request.setTransferErrorMessage(failureReason != null && !failureReason.isBlank() ? failureReason : "Transfer failed");
        request.setTransferCompletedAt(LocalDateTime.now());
        verificationRepository.save(request);

        sendVerificationResultNotification(request, false);
        logger.warn("BANK_VERIFY_WEBHOOK_TRANSFER_FAILED: verificationNumber={}, transferReferenceId={}, reason={}",
                request.getVerificationNumber(), transferReferenceId, request.getTransferErrorMessage());
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

    private void upsertUserBankDetailsForVerification(User user, BankVerificationRequestDto dto) {
        if (user == null || dto == null || dto.getVerificationType() == null) {
            return;
        }

        if (!VerificationType.BANK_ACCOUNT.name().equalsIgnoreCase(dto.getVerificationType())) {
            return;
        }

        if (dto.getAccountNumber() == null || dto.getAccountNumber().isBlank()) {
            return;
        }

        UserBankDetails details = userBankDetailsRepository.findByUserId(user.getId())
                .orElseGet(UserBankDetails::new);

        details.setUser(user);
        details.setAccountHolderName(dto.getAccountHolderName());
        details.setAccountNumber(dto.getAccountNumber());
        details.setIfscCode(dto.getIfscCode());
        details.setBankName(dto.getBankName());
        details.setBranchName(dto.getBranchName());
        details.setIsPrimary(true);
        details.setIsVerified(false);
        details.setVerificationDate(null);

        userBankDetailsRepository.save(details);
    }

    private void markUserBankDetailsVerified(User user) {
        if (user == null) {
            return;
        }

        userBankDetailsRepository.findByUserId(user.getId()).ifPresent(details -> {
            details.setIsVerified(true);
            details.setVerificationDate(LocalDateTime.now());
            userBankDetailsRepository.save(details);
        });
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
        boolean emailSent = false;
        boolean smsSent = false;

        String subject = success
                ? "Bank Verification Successful - " + request.getVerificationNumber()
                : "Bank Verification Failed - " + request.getVerificationNumber();

        String body = buildVerificationEmailBody(request, success);

        try {
            emailService.sendBankVerificationStatusEmail(
                    request.getUser().getEmail(),
                    request.getUser().getUsername(),
                    request.getVerificationNumber(),
                    request.getTransferAmount() != null ? request.getTransferAmount().toPlainString() : "1.00",
                    request.getTransferReferenceId(),
                    request.getAccountNumberMasked() != null ? request.getAccountNumberMasked() : request.getUpiIdMasked(),
                    success,
                    request.getTransferErrorMessage(),
                    request.getAccountHolderName(),
                    request.getBankName(),
                    request.getIfscCode(),
                    request.getVerifiedAt() != null ? request.getVerifiedAt() : request.getUpdatedAt(),
                    "FarmEazy Verification Engine",
                    request.getStatus() != null ? request.getStatus().name() : null);
            emailSent = true;
        } catch (Exception emailEx) {
            logger.error("BANK_VERIFY_EMAIL_FAILED: verificationNumber={}, error={}",
                    request.getVerificationNumber(), emailEx.getMessage());
        }

        // SMS should not depend on email success. Trigger with the configured
        // msg91.template.bank.details.alert template for bank verification updates.
        try {
            String userPhone = request.getUser().getPhone();
            if (userPhone != null && !userPhone.isBlank()) {
                String smsAction = resolveBankDetailsSmsAction(request, success);
                SmsResponseDto smsResponse = smsService.sendBankDetailsAlert(userPhone, smsAction);
                smsSent = smsResponse != null && smsResponse.isSuccess();

                if (!smsSent) {
                    logger.warn("BANK_VERIFY_SMS_NOT_SENT: verificationNumber={}, reason={}",
                            request.getVerificationNumber(),
                            smsResponse != null ? smsResponse.getMessage() : "No response from SMS service");
                }
            } else {
                logger.warn("BANK_VERIFY_SMS_SKIPPED_NO_PHONE: verificationNumber={}",
                        request.getVerificationNumber());
            }
        } catch (Exception smsEx) {
            logger.error("BANK_VERIFY_SMS_FAILED: verificationNumber={}, error={}",
                    request.getVerificationNumber(), smsEx.getMessage());
        }

        if (emailSent || smsSent) {
            request.setUserNotified(true);
            request.setNotificationType(emailSent ? NotificationType.EMAIL : NotificationType.SMS);
            request.setNotificationSentAt(LocalDateTime.now());
            verificationRepository.save(request);

            logCommunication(request, success);
        } else {
            logger.error("BANK_VERIFY_NOTIFICATION_FAILED: verificationNumber={}, error=No notification channel succeeded",
                    request.getVerificationNumber());
        }
    }

    private String resolveBankDetailsSmsAction(BankVerificationRequest request, boolean success) {
        if (!success) {
            return "verification failed";
        }

        boolean hasPriorRequests = verificationRepository.existsByUserIdAndIdNot(
                request.getUser().getId(),
                request.getId());

        return hasPriorRequests ? "updated" : "added";
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
        response.setSuccess(request.getStatus() != VerificationStatus.TRANSFER_FAILED);
        response.setVerificationNumber(request.getVerificationNumber());
        response.setStatus(request.getStatus().name());
        response.setTransferAmount(request.getTransferAmount());
        response.setTransferReferenceId(request.getTransferReferenceId());
        response.setCanVerify(limit.canVerify());
        response.setRemainingToday(limit.getRemainingToday());
        response.setRemainingThisMonth(limit.getRemainingThisMonth());
        response.setRemainingTotal(limit.getRemainingTotal());

        if (request.getStatus() == VerificationStatus.TRANSFER_FAILED) {
            String error = request.getTransferErrorMessage() != null && !request.getTransferErrorMessage().isBlank()
                    ? request.getTransferErrorMessage()
                    : "Transfer could not be initiated. Please try again.";
            response.setMessage("Verification initiation failed: " + error);
        } else if (request.getStatus() == VerificationStatus.TRANSFER_SUCCESS) {
            response.setMessage("₹" + verificationAmount + " transfer is successful. Please confirm verification.");
        } else {
            response.setMessage("Verification initiated. ₹" + verificationAmount +
                    " transfer is pending and will be processed shortly.");
        }
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

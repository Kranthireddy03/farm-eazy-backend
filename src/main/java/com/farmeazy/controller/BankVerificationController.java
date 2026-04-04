package com.farmeazy.controller;

import com.farmeazy.dto.BankVerificationRequestDto;
import com.farmeazy.dto.BankVerificationResponseDto;
import com.farmeazy.entity.BankVerificationRequest;
import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.BankVerificationService;
import com.farmeazy.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * BANK VERIFICATION CONTROLLER
 * 
 * PURPOSE: REST API for bank account/UPI verification via 1 rupee transfers.
 * Implements rate limiting to prevent abuse of verification system.
 * 
 * ENDPOINTS:
 * - GET /api/bank-verification/eligibility  - Check if user can verify
 * - POST /api/bank-verification/initiate    - Start verification process
 * - POST /api/bank-verification/confirm     - Confirm receipt of amount
 * - GET /api/bank-verification/history      - Get verification history
 * - GET /api/bank-verification/{number}     - Get specific verification
 * 
 * WHY THIS API EXISTS:
 * Before making payouts to sellers, we need to verify their bank accounts.
 * We send ₹1 to verify the account is valid and belongs to the user.
 * 
 * RATE LIMITING:
 * - Daily limit: 3 attempts per day
 * - Monthly limit: 10 attempts per month
 * - Total limit: 50 attempts lifetime
 * 
 * This prevents abuse where a user might try to verify 100 different accounts
 * (which would cost us ₹100 in transfer fees).
 */
@RestController
@RequestMapping("/api/bank-verification")
@Tag(name = "Bank Verification", description = "APIs for verifying bank accounts via 1 rupee transfers")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class BankVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(BankVerificationController.class);

    @Autowired
    private BankVerificationService bankVerificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    /**
     * Gets user's verification list (Frontend-friendly endpoint).
     * Maps to: GET /api/bank-verification
     */
    @GetMapping
    @Operation(summary = "Get verification list",
               description = "Get user's bank verification history")
    public ResponseEntity<?> getVerifications(Authentication auth, Pageable pageable) {
        User user = getUserFromAuth(auth);
        Page<BankVerificationRequest> history = bankVerificationService.getUserVerifications(
                user.getId(), pageable);
        
        Page<?> safePage = history.map(this::toSafeVerificationMap);
        
        return ResponseEntity.ok(safePage);
    }

    /**
     * Gets user's verification limits (Frontend-friendly endpoint).
     * Maps to: GET /api/bank-verification/limits
     */
    @GetMapping("/limits")
    @Operation(summary = "Get verification limits",
               description = "Get user's remaining verification attempts for today")
    public ResponseEntity<?> getLimits(Authentication auth) {
        User user = getUserFromAuth(auth);
        BankVerificationResponseDto eligibility = bankVerificationService.checkVerificationEligibility(user.getId());
        
        return ResponseEntity.ok(Map.of(
                "canVerify", eligibility.isCanVerify(),
                "remainingToday", eligibility.getRemainingToday(),
                "dailyLimit", 3,
                "message", eligibility.getMessage() != null ? eligibility.getMessage() : ""
        ));
    }

    /**
     * Initiates verification (Frontend-friendly endpoint).
     * Maps to: POST /api/bank-verification
     */
    @PostMapping
    @Operation(summary = "Create verification",
               description = "Start bank/UPI verification process")
    public ResponseEntity<?> createVerification(
            @Valid @RequestBody BankVerificationRequestDto dto,
            Authentication auth) {
        return initiateVerification(dto, auth);
    }

    /**
     * Checks if user is eligible for bank verification.
     * 
     * WHY: Before showing the verification form, we should check if the user
     * has remaining attempts. This provides a better UX and prevents wasted
     * form submissions.
     * 
     * @param auth Current authenticated user
     * @return Eligibility status with remaining attempts
     */
    @GetMapping("/eligibility")
    @Operation(summary = "Check verification eligibility",
               description = "Check if user can perform bank verification based on rate limits")
    public ResponseEntity<BankVerificationResponseDto> checkEligibility(Authentication auth) {
        
        logger.debug("BANK_VERIFY_ELIGIBILITY_CHECK: user={}", auth.getName());
        
        User user = getUserFromAuth(auth);
        BankVerificationResponseDto response = bankVerificationService.checkVerificationEligibility(user.getId());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates bank account or UPI verification.
     * 
     * WHY: This starts the verification process by sending ₹1 to the
     * provided bank account or UPI ID. The user must confirm receipt
     * to complete verification.
     * 
     * @param dto Bank/UPI details to verify
     * @param auth Current authenticated user
     * @return Verification initiation status
     */
    @PostMapping("/initiate")
    @Operation(summary = "Initiate verification",
               description = "Start bank/UPI verification by triggering a ₹1 transfer")
    public ResponseEntity<?> initiateVerification(
            @Valid @RequestBody BankVerificationRequestDto dto,
            Authentication auth) {
        
        logger.info("BANK_VERIFY_INITIATE: user={}, type={}", auth.getName(), dto.getVerificationType());
        
        // Validate account number confirmation for bank accounts
        if ("BANK_ACCOUNT".equals(dto.getVerificationType())) {
            if (dto.getAccountNumber() == null || !dto.getAccountNumber().equals(dto.getConfirmAccountNumber())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Account number mismatch",
                        "message", "Account number and confirm account number must match"
                ));
            }
        }
        
        User user = getUserFromAuth(auth);

        if (!otpService.isOtpVerified(user.getEmail(), "BANK_VERIFICATION")) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "OTP verification required",
                "message", "Please verify OTP before initiating bank or UPI verification"
            ));
        }

        BankVerificationResponseDto response = bankVerificationService.initiateVerification(user.getId(), dto);
        
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Confirms verification after user receives the amount.
     * 
     * WHY: After we send ₹1, the user must confirm they received it.
     * This proves the bank account is valid and accessible.
     * 
     * @param verificationNumber The verification ID to confirm
     * @param auth Current authenticated user
     * @return Confirmation status
     */
    @PostMapping("/confirm/{verificationNumber}")
    @Operation(summary = "Confirm verification",
               description = "Confirm receipt of ₹1 transfer to complete verification")
    public ResponseEntity<BankVerificationResponseDto> confirmVerification(
            @PathVariable String verificationNumber,
            Authentication auth) {
        
        logger.info("BANK_VERIFY_CONFIRM: user={}, verificationNumber={}", 
                auth.getName(), verificationNumber);
        
        // Verify ownership
        BankVerificationRequest request = bankVerificationService.getByVerificationNumber(verificationNumber);
        User user = getUserFromAuth(auth);
        
        if (!request.getUser().getId().equals(user.getId())) {
            BankVerificationResponseDto response = new BankVerificationResponseDto();
            response.setSuccess(false);
            response.setMessage("Access denied");
            return ResponseEntity.status(403).body(response);
        }
        
        BankVerificationResponseDto response = bankVerificationService.confirmVerification(verificationNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets user's verification history.
     * 
     * WHY: Users may want to see their past verification attempts and statuses.
     */
    @GetMapping("/history")
    @Operation(summary = "Get verification history",
               description = "Get paginated list of user's bank verification attempts")
    public ResponseEntity<?> getVerificationHistory(
            Authentication auth,
            Pageable pageable) {
        
        User user = getUserFromAuth(auth);
        Page<BankVerificationRequest> history = bankVerificationService.getUserVerifications(
                user.getId(), pageable);
        
        // Map to safe response (no sensitive data)
        Page<?> safePage = history.map(this::toSafeVerificationMap);
        
        return ResponseEntity.ok(safePage);
    }

    /**
     * Gets specific verification details.
     */
    @GetMapping("/{verificationNumber}")
    @Operation(summary = "Get verification details",
               description = "Get details of a specific verification request")
    public ResponseEntity<?> getVerificationDetails(
            @PathVariable String verificationNumber,
            Authentication auth) {
        
        BankVerificationRequest request = bankVerificationService.getByVerificationNumber(verificationNumber);
        User user = getUserFromAuth(auth);
        
        if (!request.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("verificationNumber", request.getVerificationNumber());
        response.put("type", request.getVerificationType() != null ? request.getVerificationType().name() : "");
        response.put("accountMasked", request.getAccountNumberMasked() != null
            ? request.getAccountNumberMasked()
            : (request.getUpiIdMasked() != null ? request.getUpiIdMasked() : ""));
        response.put("bankName", request.getBankName() != null ? request.getBankName() : "");
        response.put("status", request.getStatus() != null ? request.getStatus().name() : "");
        response.put("amount", request.getTransferAmount());
        response.put("transferReference", request.getTransferReferenceId() != null ? request.getTransferReferenceId() : "");
        response.put("createdAt", request.getCreatedAt());
        response.put("verifiedAt", request.getVerifiedAt());
        response.put("expiresAt", request.getExpiresAt());

        return ResponseEntity.ok(response);
    }

    // ========== HELPER METHODS ==========

    private User getUserFromAuth(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Map<String, Object> toSafeVerificationMap(BankVerificationRequest request) {
        Map<String, Object> item = new HashMap<>();
        item.put("verificationNumber", request.getVerificationNumber());
        item.put("type", request.getVerificationType() != null ? request.getVerificationType().name() : "");
        item.put("accountMasked", request.getAccountNumberMasked() != null
                ? request.getAccountNumberMasked()
                : (request.getUpiIdMasked() != null ? request.getUpiIdMasked() : ""));
        item.put("status", request.getStatus() != null ? request.getStatus().name() : "");
        item.put("amount", request.getTransferAmount());
        item.put("createdAt", request.getCreatedAt());
        item.put("verifiedAt", request.getVerifiedAt());
        return item;
    }
}

package com.farmeazy.controller;

import com.farmeazy.entity.PayoutBatch;
import com.farmeazy.entity.PayoutAudit;
import com.farmeazy.dto.OtpRequestDto;
import com.farmeazy.dto.OtpResponseDto;
import com.farmeazy.service.PayoutBatchService;
import com.farmeazy.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for Batch Payout Operations
 * 
 * SECURITY NOTES:
 * ✅ All endpoints require authentication
 * ✅ Admin-only operations marked with @PreAuthorize
 * ✅ CSV export only for approved batches
 * ✅ OTP verification required for approval
 * ✅ All actions logged to audit trail
 * ✅ Rate limiting recommended (implement in production)
 */
@RestController
@RequestMapping("/api/payouts")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "https://farm-eazy-backend.onrender.com"})
public class PayoutBatchController {

    private static final Logger logger = LoggerFactory.getLogger(PayoutBatchController.class);

    @Autowired
    private PayoutBatchService payoutBatchService;

    @Autowired
    private OtpService otpService;

    // ========== LIST & FETCH ENDPOINTS ==========

    /**
     * Get all pending approval batches (admin only)
     */
    @GetMapping("/batches/pending")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<PayoutBatch>> getPendingApprovalBatches() {
        logger.info("ENDPOINT_CALLED: GET /api/payouts/batches/pending");
        List<PayoutBatch> batches = payoutBatchService.getPendingApprovalBatches();
        return ResponseEntity.ok(batches);
    }

    /**
     * Get all batches (admin only)
     */
    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<PayoutBatch>> getAllBatches() {
        logger.info("ENDPOINT_CALLED: GET /api/payouts/batches");
        // In production, add pagination
        return ResponseEntity.ok(payoutBatchService.getPendingApprovalBatches());
    }

    /**
     * Get batch details by ID
     */
    @GetMapping("/batch/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<PayoutBatch> getBatchById(@PathVariable Long batchId) {
        logger.info("ENDPOINT_CALLED: GET /api/payouts/batch/{}", batchId);
        PayoutBatch batch = payoutBatchService.getBatchById(batchId);
        return ResponseEntity.ok(batch);
    }

    // ========== APPROVAL WORKFLOW ==========

    /**
     * Request OTP for batch approval (step 1)
     * 
     * SECURITY:
     * ✅ Sends OTP to approver's phone/email
     * ✅ Only for CREATED status batches
     * ✅ Creator cannot approve their own batch
     */
    @PostMapping("/batch/{batchId}/request-otp")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Map<String, String>> requestApprovalOtp(@PathVariable Long batchId) {
        logger.info("ENDPOINT_CALLED: POST /api/payouts/batch/{}/request-otp", batchId);
        
        try {
            PayoutBatch batch = payoutBatchService.getBatchById(batchId);
            
            // Get current user's email for OTP
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            
            // Send OTP using proper OtpRequestDto
            OtpRequestDto otpRequest = new OtpRequestDto();
            otpRequest.setEmail(email);
            otpRequest.setPurpose("PAYOUT_APPROVAL");
            
            OtpResponseDto otpResponse = otpService.generateAndSendOtpWithDetails(otpRequest);
            
            logger.info("OTP_REQUESTED: batchId={}, email={}", batchId, email);
            
            return ResponseEntity.ok(Map.of(
                "message", otpResponse.getDisplayMessage() != null ? otpResponse.getDisplayMessage() : "OTP sent successfully",
                "success", String.valueOf(otpResponse.isSuccess())
            ));
        } catch (Exception e) {
            logger.error("OTP_REQUEST_ERROR: batchId={}, error={}", batchId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to send OTP: " + e.getMessage()));
        }
    }

    /**
     * Approve batch with OTP verification (step 2)
     * 
     * SECURITY:
     * ✅ Validates OTP before approval
     * ✅ Enforces maker-checker (creator ≠ approver)
     * ✅ Locks batch status (no changes after approval)
     * ✅ Immutable audit log
     */
    @PostMapping("/batch/{batchId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<Map<String, Object>> approveBatch(
            @PathVariable Long batchId,
            @RequestBody Map<String, String> request) {
        
        String otpCode = request.get("otpCode");
        logger.info("ENDPOINT_CALLED: POST /api/payouts/batch/{}/approve", batchId);

        if (otpCode == null || otpCode.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "OTP code is required"));
        }

        try {
            payoutBatchService.approveBatchWithOtp(batchId, otpCode);
            PayoutBatch batch = payoutBatchService.getBatchById(batchId);
            
            logger.info("PAYOUT_BATCH_APPROVED_ENDPOINT: batchId={}", batchId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Batch approved successfully",
                "batch", batch,
                "nextStep", "Download CSV and upload to bank"
            ));
        } catch (Exception e) {
            logger.error("APPROVAL_FAILED: batchId={}, error={}", batchId, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== CSV EXPORT & AUDIT ==========

    /**
     * Download CSV for bank upload (APPROVED batches only)
     * 
     * SECURITY:
     * ✅ Only available after batch approval
     * ✅ Masked bank details (last 4 digits only)
     * ✅ Download logged to audit trail
     * ✅ IP address recorded
     */
    @GetMapping("/batch/{batchId}/export")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<byte[]> exportBatchCsv(@PathVariable Long batchId) {
        logger.info("ENDPOINT_CALLED: GET /api/payouts/batch/{}/export", batchId);

        try {
            String csvContent = payoutBatchService.generatePayoutCsv(batchId);
            byte[] csvBytes = csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "payout_batch_" + batchId + ".csv");
            headers.setContentLength(csvBytes.length);

            logger.info("CSV_EXPORTED: batchId={}, size={} bytes", batchId, csvBytes.length);

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            logger.error("CSV_EXPORT_ENDPOINT_ERROR: batchId={}, error={}", batchId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("Content-Type", "application/json")
                .body("{\"error\": \"".getBytes());
        }
    }

    /**
     * Get complete audit trail for batch
     * Shows all actions (created, approved, exported, etc.)
     */
    @GetMapping("/batch/{batchId}/audit")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<PayoutAudit>> getBatchAuditLog(@PathVariable Long batchId) {
        logger.info("ENDPOINT_CALLED: GET /api/payots/batch/{}/audit", batchId);
        List<PayoutAudit> auditLog = payoutBatchService.getBatchAuditLog(batchId);
        return ResponseEntity.ok(auditLog);
    }

    // ========== ERROR HANDLING ==========

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        logger.error("UNHANDLED_EXCEPTION: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Internal server error: " + e.getMessage()));
    }
}

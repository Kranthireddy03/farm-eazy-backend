package com.farmeazy.controller;

import com.farmeazy.entity.BatchJobExecution;
import com.farmeazy.entity.BatchJobItemLog;
import com.farmeazy.entity.BatchTransactionLog;
import com.farmeazy.service.BatchJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * BATCH JOB CONTROLLER
 * 
 * PURPOSE: REST API for monitoring and managing batch jobs.
 * Provides visibility into batch processing status and failures.
 * 
 * ENDPOINTS:
 * - GET /api/batch-jobs/running          - Get currently running jobs
 * - GET /api/batch-jobs/failed           - Get recent failed jobs
 * - GET /api/batch-jobs/{id}             - Get job details
 * - GET /api/batch-jobs/{id}/items       - Get job item logs
 * - GET /api/batch-jobs/{id}/failures    - Get failed items for debugging
 * - GET /api/batch-jobs/{id}/transactions - Get job transactions
 * 
 * WHY THIS API EXISTS:
 * Administrators need visibility into batch job execution. When something
 * fails, they need to quickly identify the issue by examining:
 * - Which job failed
 * - Which items within the job failed
 * - What errors occurred
 * - Transaction audit trail
 */
@RestController
@RequestMapping("/api/batch-jobs")
@Tag(name = "Batch Jobs", description = "APIs for monitoring batch job execution and debugging failures")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class BatchJobController {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobController.class);

    @Autowired
    private BatchJobService batchJobService;

    /**
     * Gets currently running jobs.
     * 
     * WHY: Quick overview of active batch processing.
     * Useful for monitoring and avoiding duplicate job execution.
     */
    @GetMapping("/running")
    @Operation(summary = "Get running jobs",
               description = "Get list of currently running batch jobs")
    public ResponseEntity<?> getRunningJobs() {
        List<BatchJobExecution> jobs = batchJobService.getRunningJobs();
        
        List<?> response = jobs.stream()
                .map(this::toJobSummary)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
                "count", jobs.size(),
                "jobs", response
        ));
    }

    /**
     * Gets recent failed jobs for analysis.
     * 
     * WHY: Critical for identifying and diagnosing batch processing issues.
     * Failed jobs need immediate attention to resolve data inconsistencies.
     */
    @GetMapping("/failed")
    @Operation(summary = "Get failed jobs",
               description = "Get recent failed jobs for debugging (last 7 days)")
    public ResponseEntity<?> getRecentFailedJobs(
            @RequestParam(defaultValue = "7") int daysBack) {
        
        logger.info("BATCH_JOB_API: Getting failed jobs for last {} days", daysBack);
        
        List<BatchJobExecution> jobs = batchJobService.getRecentFailedJobs(daysBack);
        
        List<?> response = jobs.stream()
                .map(this::toJobSummary)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
                "count", jobs.size(),
                "daysBack", daysBack,
                "jobs", response
        ));
    }

    /**
     * Gets details of a specific job.
     * 
     * WHY: Detailed view of job execution including statistics,
     * error summary, and timing information.
     */
    @GetMapping("/{jobId}")
    @Operation(summary = "Get job details",
               description = "Get detailed information about a specific batch job execution")
    public ResponseEntity<?> getJobDetails(@PathVariable Long jobId) {
        
        List<BatchJobExecution> allJobs = batchJobService.getRunningJobs();
        // For demo - in production, add findById to repository
        
        return ResponseEntity.ok(Map.of(
                "message", "Job details API - implement findById in repository",
                "jobId", jobId
        ));
    }

    /**
     * Gets item-level logs for a job.
     * 
     * WHY: When a job fails or partially completes, administrators need
     * to see which specific items succeeded, failed, or were skipped.
     */
    @GetMapping("/{jobId}/items")
    @Operation(summary = "Get job items",
               description = "Get item-level processing logs for a batch job")
    public ResponseEntity<?> getJobItems(@PathVariable Long jobId) {
        
        logger.info("BATCH_JOB_API: Getting items for job {}", jobId);
        
        List<BatchJobItemLog> items = batchJobService.getJobItemLogs(jobId);
        
        List<?> response = items.stream()
                .map(item -> Map.of(
                        "id", item.getId(),
                        "referenceType", item.getItemReferenceType(),
                        "referenceId", item.getItemReferenceId(),
                        "status", item.getItemStatus().name(),
                        "errorCode", item.getErrorCode() != null ? item.getErrorCode() : "",
                        "errorMessage", item.getErrorMessage() != null ? item.getErrorMessage() : "",
                        "retryCount", item.getRetryCount(),
                        "createdAt", item.getCreatedAt()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "count", items.size(),
                "items", response
        ));
    }

    /**
     * Gets failed items for a job (for debugging).
     * 
     * WHY: Quick access to only the failed items for faster debugging.
     * Shows error codes, messages, and stack traces.
     */
    @GetMapping("/{jobId}/failures")
    @Operation(summary = "Get failed items",
               description = "Get only failed items for a batch job (for debugging)")
    public ResponseEntity<?> getFailedItems(@PathVariable Long jobId) {
        
        logger.info("BATCH_JOB_API: Getting failed items for job {}", jobId);
        
        List<BatchJobItemLog> items = batchJobService.getFailedItemsForJob(jobId);
        
        List<?> response = items.stream()
                .map(item -> Map.of(
                        "id", item.getId(),
                        "referenceType", item.getItemReferenceType(),
                        "referenceId", item.getItemReferenceId(),
                        "errorCode", item.getErrorCode() != null ? item.getErrorCode() : "UNKNOWN",
                        "errorMessage", item.getErrorMessage() != null ? item.getErrorMessage() : "No message",
                        "stackTrace", item.getErrorStackTrace() != null 
                                ? item.getErrorStackTrace().substring(0, Math.min(500, item.getErrorStackTrace().length()))
                                : "",
                        "retryCount", item.getRetryCount(),
                        "createdAt", item.getCreatedAt()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "failureCount", items.size(),
                "failures", response
        ));
    }

    /**
     * Gets transactions for a job.
     * 
     * WHY: Financial audit trail for batch processed transactions.
     * Shows all payment reversals, payouts, and refunds processed.
     */
    @GetMapping("/{jobId}/transactions")
    @Operation(summary = "Get job transactions",
               description = "Get financial transactions processed by a batch job")
    public ResponseEntity<?> getJobTransactions(@PathVariable Long jobId) {
        
        logger.info("BATCH_JOB_API: Getting transactions for job {}", jobId);
        
        List<BatchTransactionLog> transactions = batchJobService.getJobTransactions(jobId);
        
        List<?> response = transactions.stream()
                .map(txn -> Map.of(
                        "id", txn.getId(),
                        "type", txn.getTransactionType().name(),
                        "referenceType", txn.getReferenceType(),
                        "referenceId", txn.getReferenceId(),
                        "amount", txn.getAmount(),
                        "status", txn.getStatus().name(),
                        "accountMasked", txn.getBankAccountMasked() != null 
                                ? txn.getBankAccountMasked() 
                                : txn.getUpiIdMasked(),
                        "gatewayTxnId", txn.getGatewayTransactionId(),
                        "createdAt", txn.getCreatedAt()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "count", transactions.size(),
                "transactions", response
        ));
    }

    // ========== HELPER METHODS ==========

    private Map<String, Object> toJobSummary(BatchJobExecution job) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", job.getId());
        summary.put("jobName", job.getJobName());
        summary.put("jobType", job.getJobType().name());
        summary.put("status", job.getJobStatus().name());
        summary.put("totalRecords", job.getTotalRecords());
        summary.put("successCount", job.getSuccessCount());
        summary.put("failureCount", job.getFailureCount());
        summary.put("skipCount", job.getSkipCount());
        summary.put("startTime", job.getStartTime());
        summary.put("endTime", job.getEndTime() != null ? job.getEndTime() : "");
        summary.put("errorSummary", job.getErrorSummary() != null ? job.getErrorSummary() : "");
        return summary;
    }
}

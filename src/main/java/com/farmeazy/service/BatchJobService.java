package com.farmeazy.service;

import com.farmeazy.entity.BatchJobExecution;
import com.farmeazy.entity.BatchJobExecution.JobStatus;
import com.farmeazy.entity.BatchJobExecution.JobType;
import com.farmeazy.entity.BatchJobItemLog;
import com.farmeazy.entity.BatchJobItemLog.ItemStatus;
import com.farmeazy.entity.BatchTransactionLog;
import com.farmeazy.entity.BatchTransactionLog.TransactionStatus;
import com.farmeazy.entity.BatchTransactionLog.TransactionType;
import com.farmeazy.entity.User;
import com.farmeazy.repository.BatchJobExecutionRepository;
import com.farmeazy.repository.BatchJobItemLogRepository;
import com.farmeazy.repository.BatchTransactionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * BATCH JOB SERVICE
 * 
 * PURPOSE: Central service for managing batch job execution.
 * Handles payment reversals, payouts, refunds, and other scheduled operations.
 * 
 * KEY FEATURES:
 * - Comprehensive logging with MDC for correlation
 * - Success/failure stamping in database
 * - Item-level tracking for debugging
 * - Error summary aggregation
 * - Transaction logging (separate table)
 * 
 * SECURITY:
 * - No sensitive data in logs (account numbers masked)
 * - MDC cleared after each job
 * - Audit trail for all operations
 */
@Service
public class BatchJobService {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    @Autowired
    private BatchJobExecutionRepository jobExecutionRepository;

    @Autowired
    private BatchJobItemLogRepository itemLogRepository;

    @Autowired
    private BatchTransactionLogRepository transactionLogRepository;

    @Value("${farmeazy.batch.max-retries:3}")
    private int maxRetries;

    private String serverInstance;

    /**
     * Initialize server instance identifier.
     */
    public BatchJobService() {
        try {
            this.serverInstance = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            this.serverInstance = "unknown-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    // ========== JOB EXECUTION MANAGEMENT ==========

    /**
     * Starts a new batch job execution.
     * Sets up MDC for correlated logging throughout the job.
     */
    @Transactional
    public BatchJobExecution startJob(JobType jobType, String jobName, String triggeredBy) {
        String correlationId = UUID.randomUUID().toString();
        
        // Set MDC for correlated logging
        MDC.put("jobCorrelationId", correlationId);
        MDC.put("jobType", jobType.name());
        MDC.put("jobName", jobName);

        logger.info("BATCH_JOB_START: Starting {} - {}", jobType, jobName);
        auditLogger.info("BATCH_JOB_STARTED: type={}, name={}, triggeredBy={}, correlationId={}", 
                jobType, jobName, triggeredBy, correlationId);

        BatchJobExecution job = new BatchJobExecution();
        job.setJobName(jobName);
        job.setJobType(jobType);
        job.setJobStatus(JobStatus.STARTED);
        job.setStartTime(LocalDateTime.now());
        job.setTriggeredBy(triggeredBy != null ? triggeredBy : "SCHEDULER");
        job.setServerInstance(serverInstance);
        
        job = jobExecutionRepository.save(job);
        
        logger.debug("BATCH_JOB_CREATED: jobId={}", job.getId());
        return job;
    }

    /**
     * Marks job as running and updates total records.
     */
    @Transactional
    public void markJobRunning(BatchJobExecution job, int totalRecords) {
        job.setJobStatus(JobStatus.RUNNING);
        job.setTotalRecords(totalRecords);
        jobExecutionRepository.save(job);
        
        logger.info("BATCH_JOB_RUNNING: jobId={}, totalRecords={}", job.getId(), totalRecords);
    }

    /**
     * Completes a batch job and records final statistics.
     */
    @Transactional
    public void completeJob(BatchJobExecution job, String errorSummary) {
        job.setEndTime(LocalDateTime.now());
        job.setProcessedRecords(job.getSuccessCount() + job.getFailureCount() + job.getSkipCount());
        
        if (job.getFailureCount() > 0 && job.getSuccessCount() > 0) {
            job.setJobStatus(JobStatus.PARTIALLY_COMPLETED);
        } else if (job.getFailureCount() > 0) {
            job.setJobStatus(JobStatus.FAILED);
        } else {
            job.setJobStatus(JobStatus.COMPLETED);
        }
        
        if (errorSummary != null && !errorSummary.isEmpty()) {
            job.setErrorSummary(truncate(errorSummary, 2000));
        }
        
        jobExecutionRepository.save(job);
        
        long durationMs = java.time.Duration.between(job.getStartTime(), job.getEndTime()).toMillis();
        
        logger.info("BATCH_JOB_COMPLETED: jobId={}, status={}, total={}, success={}, failed={}, skipped={}, duration={}ms",
                job.getId(), job.getJobStatus(), job.getTotalRecords(),
                job.getSuccessCount(), job.getFailureCount(), job.getSkipCount(), durationMs);
        
        auditLogger.info("BATCH_JOB_FINISHED: jobId={}, status={}, successRate={}%",
                job.getId(), job.getJobStatus(),
                job.getTotalRecords() > 0 ? (job.getSuccessCount() * 100 / job.getTotalRecords()) : 0);
        
        // Clear MDC after job completion
        MDC.remove("jobCorrelationId");
        MDC.remove("jobType");
        MDC.remove("jobName");
    }

    /**
     * Marks job as failed with error details.
     */
    @Transactional
    public void failJob(BatchJobExecution job, Exception e) {
        job.setEndTime(LocalDateTime.now());
        job.setJobStatus(JobStatus.FAILED);
        job.setErrorSummary(truncate(e.getMessage(), 2000));
        jobExecutionRepository.save(job);
        
        logger.error("BATCH_JOB_FAILED: jobId={}, error={}", job.getId(), e.getMessage(), e);
        auditLogger.error("BATCH_JOB_ERROR: jobId={}, errorType={}, errorMessage={}",
                job.getId(), e.getClass().getSimpleName(), maskSensitiveData(e.getMessage()));
        
        MDC.clear();
    }

    // ========== ITEM LEVEL LOGGING ==========

    /**
     * Logs successful processing of an item.
     */
    @Transactional
    public BatchJobItemLog logItemSuccess(BatchJobExecution job, String referenceType, 
            String referenceId, String outputData) {
        
        BatchJobItemLog item = new BatchJobItemLog();
        item.setJobExecution(job);
        item.setItemReferenceType(referenceType);
        item.setItemReferenceId(referenceId);
        item.setItemStatus(ItemStatus.SUCCESS);
        item.setProcessingStartTime(LocalDateTime.now());
        item.setProcessingEndTime(LocalDateTime.now());
        item.setOutputData(outputData);
        
        item = itemLogRepository.save(item);
        
        job.setSuccessCount(job.getSuccessCount() + 1);
        jobExecutionRepository.save(job);
        
        logger.debug("BATCH_ITEM_SUCCESS: jobId={}, reference={}-{}", 
                job.getId(), referenceType, referenceId);
        
        return item;
    }

    /**
     * Logs failed processing of an item with error details.
     * Critical for debugging failures.
     */
    @Transactional
    public BatchJobItemLog logItemFailure(BatchJobExecution job, String referenceType,
            String referenceId, String errorCode, String errorMessage, String stackTrace) {
        
        BatchJobItemLog item = new BatchJobItemLog();
        item.setJobExecution(job);
        item.setItemReferenceType(referenceType);
        item.setItemReferenceId(referenceId);
        item.setItemStatus(ItemStatus.FAILED);
        item.setProcessingStartTime(LocalDateTime.now());
        item.setProcessingEndTime(LocalDateTime.now());
        item.setErrorCode(errorCode);
        item.setErrorMessage(truncate(maskSensitiveData(errorMessage), 1000));
        item.setErrorStackTrace(truncate(stackTrace, 4000));
        
        item = itemLogRepository.save(item);
        
        job.setFailureCount(job.getFailureCount() + 1);
        jobExecutionRepository.save(job);
        
        logger.warn("BATCH_ITEM_FAILED: jobId={}, reference={}-{}, errorCode={}, error={}",
                job.getId(), referenceType, referenceId, errorCode, maskSensitiveData(errorMessage));
        
        return item;
    }

    /**
     * Logs skipped item with reason.
     */
    @Transactional
    public BatchJobItemLog logItemSkipped(BatchJobExecution job, String referenceType,
            String referenceId, String reason) {
        
        BatchJobItemLog item = new BatchJobItemLog();
        item.setJobExecution(job);
        item.setItemReferenceType(referenceType);
        item.setItemReferenceId(referenceId);
        item.setItemStatus(ItemStatus.SKIPPED);
        item.setProcessingEndTime(LocalDateTime.now());
        item.setOutputData("{\"skipReason\": \"" + reason + "\"}");
        
        item = itemLogRepository.save(item);
        
        job.setSkipCount(job.getSkipCount() + 1);
        jobExecutionRepository.save(job);
        
        logger.debug("BATCH_ITEM_SKIPPED: jobId={}, reference={}-{}, reason={}",
                job.getId(), referenceType, referenceId, reason);
        
        return item;
    }

    // ========== TRANSACTION LOGGING ==========

    /**
     * Logs a batch transaction (payment, refund, payout, etc.).
     * All financial operations must be logged here.
     */
    @Transactional
    public BatchTransactionLog logTransaction(BatchJobExecution job, TransactionType type,
            String referenceType, Long referenceId, User user, BigDecimal amount,
            TransactionStatus status, String gatewayTransactionId, 
            String maskedAccount, String maskedUpi, String notes) {
        
        BatchTransactionLog txn = new BatchTransactionLog();
        txn.setBatchJob(job);
        txn.setTransactionType(type);
        txn.setReferenceType(referenceType);
        txn.setReferenceId(referenceId);
        txn.setUser(user);
        txn.setAmount(amount);
        txn.setStatus(status);
        txn.setGatewayTransactionId(gatewayTransactionId);
        txn.setBankAccountMasked(maskedAccount);
        txn.setUpiIdMasked(maskedUpi);
        txn.setNotes(notes);
        txn.setPaymentGateway("Razorpay");
        
        txn = transactionLogRepository.save(txn);
        
        // Log without sensitive data
        logger.info("BATCH_TXN_LOGGED: txnId={}, type={}, reference={}-{}, amount={}, status={}, account={}",
                txn.getId(), type, referenceType, referenceId, amount, status,
                maskedAccount != null ? maskedAccount : maskedUpi);
        
        auditLogger.info("BATCH_TRANSACTION: txnId={}, type={}, userId={}, amount={}, status={}",
                txn.getId(), type, user != null ? user.getId() : "N/A", amount, status);
        
        return txn;
    }

    /**
     * Updates transaction status.
     */
    @Transactional
    public void updateTransactionStatus(BatchTransactionLog txn, TransactionStatus newStatus,
            String gatewayResponseCode, String gatewayResponseMessage) {
        
        String previousStatus = txn.getStatus().name();
        txn.setPreviousStatus(previousStatus);
        txn.setStatus(newStatus);
        txn.setGatewayResponseCode(gatewayResponseCode);
        txn.setGatewayResponseMessage(truncate(gatewayResponseMessage, 500));
        txn.setAttemptNumber(txn.getAttemptNumber() + 1);
        
        transactionLogRepository.save(txn);
        
        logger.info("BATCH_TXN_UPDATED: txnId={}, previousStatus={}, newStatus={}, responseCode={}",
                txn.getId(), previousStatus, newStatus, gatewayResponseCode);
    }

    // ========== QUERY METHODS ==========

    /**
     * Get recent failed jobs for analysis.
     */
    public List<BatchJobExecution> getRecentFailedJobs(int daysBack) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);
        return jobExecutionRepository.findRecentFailedJobs(since);
    }

    /**
     * Get item logs for a specific job (for debugging).
     */
    public List<BatchJobItemLog> getJobItemLogs(Long jobId) {
        return itemLogRepository.findByJobExecutionId(jobId);
    }

    /**
     * Get failed items for a job (for debugging failures).
     */
    public List<BatchJobItemLog> getFailedItemsForJob(Long jobId) {
        return itemLogRepository.findByJobExecutionIdAndItemStatus(jobId, ItemStatus.FAILED);
    }

    /**
     * Get transactions for a job.
     */
    public List<BatchTransactionLog> getJobTransactions(Long jobId) {
        return transactionLogRepository.findByBatchJobId(jobId);
    }

    /**
     * Get current running jobs.
     */
    public List<BatchJobExecution> getRunningJobs() {
        return jobExecutionRepository.findRunningJobs();
    }

    // ========== UTILITY METHODS ==========

    /**
     * Truncates string to max length.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }

    /**
     * Masks sensitive data in logs.
     * IMPORTANT: Never log full account numbers, passwords, or PII.
     */
    private String maskSensitiveData(String data) {
        if (data == null) return null;
        
        // Mask potential account numbers (10+ digit sequences)
        data = data.replaceAll("\\b\\d{10,}\\b", "****");
        
        // Mask potential card numbers
        data = data.replaceAll("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b", "****-****-****-****");
        
        // Mask email addresses partially
        data = data.replaceAll("([a-zA-Z0-9])[a-zA-Z0-9.]+@", "$1***@");
        
        // Mask UPI IDs
        data = data.replaceAll("([a-zA-Z0-9])[a-zA-Z0-9.]+@[a-z]+", "$1***@***");
        
        return data;
    }

    /**
     * Creates masked account number for safe logging.
     */
    public String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Creates masked UPI ID for safe logging.
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
}

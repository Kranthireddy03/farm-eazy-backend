package com.farmeazy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * BATCH JOB EXECUTION ENTITY
 * 
 * PURPOSE: Tracks execution of batch jobs for payment reversal, payout processing,
 * refunds, notifications, and other scheduled operations.
 * 
 * KEY FEATURES:
 * - Records start/end time and duration
 * - Tracks success/failure counts
 * - Stores error summaries for debugging
 * - Maintains audit trail for compliance
 */
@Entity
@Table(name = "batch_job_execution")
public class BatchJobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false)
    private JobStatus jobStatus;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_records")
    private Integer totalRecords = 0;

    @Column(name = "processed_records")
    private Integer processedRecords = 0;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "failure_count")
    private Integer failureCount = 0;

    @Column(name = "skip_count")
    private Integer skipCount = 0;

    @Column(name = "error_summary", length = 2000)
    private String errorSummary;

    @Column(name = "execution_parameters", columnDefinition = "JSON")
    private String executionParameters;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy = "SCHEDULER";

    @Column(name = "server_instance", length = 100)
    private String serverInstance;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // No-args constructor
    public BatchJobExecution() {
    }

    // All-args constructor
    public BatchJobExecution(Long id, String jobName, JobType jobType, JobStatus jobStatus,
                              LocalDateTime startTime, LocalDateTime endTime, Integer totalRecords,
                              Integer processedRecords, Integer successCount, Integer failureCount,
                              Integer skipCount, String errorSummary, String executionParameters,
                              String triggeredBy, String serverInstance, LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        this.id = id;
        this.jobName = jobName;
        this.jobType = jobType;
        this.jobStatus = jobStatus;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalRecords = totalRecords;
        this.processedRecords = processedRecords;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.skipCount = skipCount;
        this.errorSummary = errorSummary;
        this.executionParameters = executionParameters;
        this.triggeredBy = triggeredBy;
        this.serverInstance = serverInstance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(Integer processedRecords) {
        this.processedRecords = processedRecords;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public Integer getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(Integer skipCount) {
        this.skipCount = skipCount;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }

    public String getExecutionParameters() {
        return executionParameters;
    }

    public void setExecutionParameters(String executionParameters) {
        this.executionParameters = executionParameters;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public String getServerInstance() {
        return serverInstance;
    }

    public void setServerInstance(String serverInstance) {
        this.serverInstance = serverInstance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Job Type Enum
    public enum JobType {
        PAYMENT_REVERSAL,
        PAYOUT_PROCESSING,
        REFUND_PROCESSING,
        NOTIFICATION_RETRY,
        BANK_VERIFICATION,
        CLEANUP,
        REPORT_GENERATION
    }

    // Job Status Enum
    public enum JobStatus {
        STARTED,
        RUNNING,
        COMPLETED,
        FAILED,
        PARTIALLY_COMPLETED
    }
}

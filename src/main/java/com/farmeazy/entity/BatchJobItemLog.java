package com.farmeazy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * BATCH JOB ITEM LOG ENTITY
 * 
 * PURPOSE: Tracks individual item processing within a batch job.
 * Provides detailed logging for each record processed, including
 * error details for failure analysis.
 * 
 * KEY FEATURES:
 * - Links to parent batch job execution
 * - Stores item-level success/failure status
 * - Captures error codes and stack traces
 * - Tracks retry attempts
 */
@Entity
@Table(name = "batch_job_item_log")
public class BatchJobItemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_execution_id", nullable = false)
    private BatchJobExecution jobExecution;

    @Column(name = "item_reference_type", nullable = false, length = 50)
    private String itemReferenceType;

    @Column(name = "item_reference_id", nullable = false, length = 100)
    private String itemReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    private ItemStatus itemStatus;

    @Column(name = "processing_start_time")
    private LocalDateTime processingStartTime;

    @Column(name = "processing_end_time")
    private LocalDateTime processingEndTime;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "input_data", columnDefinition = "JSON")
    private String inputData;

    @Column(name = "output_data", columnDefinition = "JSON")
    private String outputData;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // No-args constructor
    public BatchJobItemLog() {
    }

    // All-args constructor
    public BatchJobItemLog(Long id, BatchJobExecution jobExecution, String itemReferenceType,
                            String itemReferenceId, ItemStatus itemStatus, LocalDateTime processingStartTime,
                            LocalDateTime processingEndTime, String errorCode, String errorMessage,
                            String errorStackTrace, Integer retryCount, String inputData,
                            String outputData, LocalDateTime createdAt) {
        this.id = id;
        this.jobExecution = jobExecution;
        this.itemReferenceType = itemReferenceType;
        this.itemReferenceId = itemReferenceId;
        this.itemStatus = itemStatus;
        this.processingStartTime = processingStartTime;
        this.processingEndTime = processingEndTime;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorStackTrace = errorStackTrace;
        this.retryCount = retryCount;
        this.inputData = inputData;
        this.outputData = outputData;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BatchJobExecution getJobExecution() {
        return jobExecution;
    }

    public void setJobExecution(BatchJobExecution jobExecution) {
        this.jobExecution = jobExecution;
    }

    public String getItemReferenceType() {
        return itemReferenceType;
    }

    public void setItemReferenceType(String itemReferenceType) {
        this.itemReferenceType = itemReferenceType;
    }

    public String getItemReferenceId() {
        return itemReferenceId;
    }

    public void setItemReferenceId(String itemReferenceId) {
        this.itemReferenceId = itemReferenceId;
    }

    public ItemStatus getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(ItemStatus itemStatus) {
        this.itemStatus = itemStatus;
    }

    public LocalDateTime getProcessingStartTime() {
        return processingStartTime;
    }

    public void setProcessingStartTime(LocalDateTime processingStartTime) {
        this.processingStartTime = processingStartTime;
    }

    public LocalDateTime getProcessingEndTime() {
        return processingEndTime;
    }

    public void setProcessingEndTime(LocalDateTime processingEndTime) {
        this.processingEndTime = processingEndTime;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getOutputData() {
        return outputData;
    }

    public void setOutputData(String outputData) {
        this.outputData = outputData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Item Status Enum
    public enum ItemStatus {
        SUCCESS,
        FAILED,
        SKIPPED,
        PENDING
    }
}

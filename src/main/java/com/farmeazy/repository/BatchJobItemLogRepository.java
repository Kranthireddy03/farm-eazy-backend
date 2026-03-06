package com.farmeazy.repository;

import com.farmeazy.entity.BatchJobItemLog;
import com.farmeazy.entity.BatchJobItemLog.ItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BATCH JOB ITEM LOG REPOSITORY
 * 
 * PURPOSE: Data access layer for batch job item-level logging.
 * Provides methods to analyze individual item processing results.
 */
@Repository
public interface BatchJobItemLogRepository extends JpaRepository<BatchJobItemLog, Long> {

    /**
     * Find items by job execution ID.
     */
    List<BatchJobItemLog> findByJobExecutionId(Long jobExecutionId);

    /**
     * Find items by job execution ID with pagination.
     */
    Page<BatchJobItemLog> findByJobExecutionIdOrderByCreatedAtDesc(Long jobExecutionId, Pageable pageable);

    /**
     * Find failed items by job execution ID.
     */
    List<BatchJobItemLog> findByJobExecutionIdAndItemStatus(Long jobExecutionId, ItemStatus itemStatus);

    /**
     * Find recent failures for debugging.
     */
    @Query("SELECT b FROM BatchJobItemLog b WHERE b.itemStatus = 'FAILED' " +
           "AND b.createdAt >= :since ORDER BY b.createdAt DESC")
    List<BatchJobItemLog> findRecentFailures(@Param("since") LocalDateTime since);

    /**
     * Find items by reference (e.g., ORDER_123).
     */
    List<BatchJobItemLog> findByItemReferenceTypeAndItemReferenceId(
            String itemReferenceType, String itemReferenceId);

    /**
     * Count items by status for a job.
     */
    @Query("SELECT b.itemStatus, COUNT(b) FROM BatchJobItemLog b " +
           "WHERE b.jobExecution.id = :jobId GROUP BY b.itemStatus")
    List<Object[]> countItemsByStatus(@Param("jobId") Long jobId);

    /**
     * Find items with specific error code.
     */
    List<BatchJobItemLog> findByErrorCodeOrderByCreatedAtDesc(String errorCode);
}

package com.farmeazy.repository;

import com.farmeazy.entity.BatchTransactionLog;
import com.farmeazy.entity.BatchTransactionLog.TransactionStatus;
import com.farmeazy.entity.BatchTransactionLog.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BATCH TRANSACTION LOG REPOSITORY
 * 
 * PURPOSE: Data access layer for batch transaction audit trail.
 * Provides methods to query and analyze financial batch transactions.
 */
@Repository
public interface BatchTransactionLogRepository extends JpaRepository<BatchTransactionLog, Long> {

    /**
     * Find transactions by batch job ID.
     */
    List<BatchTransactionLog> findByBatchJobId(Long batchJobId);

    /**
     * Find transactions by type and status.
     */
    List<BatchTransactionLog> findByTransactionTypeAndStatus(
            TransactionType transactionType, TransactionStatus status);

    /**
     * Find transactions by reference.
     */
    List<BatchTransactionLog> findByReferenceTypeAndReferenceId(
            String referenceType, Long referenceId);

    /**
     * Find transactions by user ID.
     */
    Page<BatchTransactionLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find failed transactions for retry.
     */
    @Query("SELECT b FROM BatchTransactionLog b WHERE b.status = 'FAILED' " +
           "AND b.attemptNumber < :maxAttempts AND b.createdAt >= :since " +
           "ORDER BY b.createdAt ASC")
    List<BatchTransactionLog> findFailedTransactionsForRetry(
            @Param("maxAttempts") int maxAttempts,
            @Param("since") LocalDateTime since);

    /**
     * Sum amounts by transaction type.
     */
    @Query("SELECT b.transactionType, SUM(b.amount) FROM BatchTransactionLog b " +
           "WHERE b.status = 'SUCCESS' AND b.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY b.transactionType")
    List<Object[]> sumAmountsByType(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count transactions by status.
     */
    @Query("SELECT b.status, COUNT(b) FROM BatchTransactionLog b " +
           "WHERE b.createdAt >= :since GROUP BY b.status")
    List<Object[]> countByStatus(@Param("since") LocalDateTime since);
}

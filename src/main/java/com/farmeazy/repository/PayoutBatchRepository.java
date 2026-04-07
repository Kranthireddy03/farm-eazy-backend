package com.farmeazy.repository;

import com.farmeazy.entity.PayoutBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface PayoutBatchRepository extends JpaRepository<PayoutBatch, Long> {

    /**
     * Find batch by date (ONE per day rule)
     */
    Optional<PayoutBatch> findByBatchDate(LocalDate batchDate);

    /**
     * Check if batch exists for today
     */
    boolean existsByBatchDate(LocalDate batchDate);

    /**
     * Get all batches by status
     */
    List<PayoutBatch> findByStatusOrderByCreatedAtDesc(PayoutBatch.BatchStatus status);

    /**
     * Get all pending/approved batches for admin review
     */
    @Query("SELECT b FROM PayoutBatch b WHERE b.status IN ('CREATED', 'APPROVED') ORDER BY b.createdAt DESC")
    List<PayoutBatch> findPendingApprovalBatches();

    /**
     * Get batches created/approved within date range
     */
    @Query("SELECT b FROM PayoutBatch b WHERE b.batchDate BETWEEN ?1 AND ?2 ORDER BY b.batchDate DESC")
    List<PayoutBatch> findBatchesByDateRange(LocalDate fromDate, LocalDate toDate);
}

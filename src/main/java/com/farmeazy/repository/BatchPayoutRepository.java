package com.farmeazy.repository;

import com.farmeazy.entity.BatchPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BatchPayout entities
 * Handles data access for batch payout records
 */
@Repository
public interface BatchPayoutRepository extends JpaRepository<BatchPayout, Long> {

    /**
     * Find all payouts for a batch
     */
    List<BatchPayout> findByBatchId(Long batchId);

    /**
     * Find all payouts with a specific status
     */
    List<BatchPayout> findByStatus(BatchPayout.PayoutStatus status);

    /**
     * Find pending payouts for a vendor
     */
    List<BatchPayout> findByVendorIdAndStatus(Long vendorId, BatchPayout.PayoutStatus status);

    /**
     * Find payouts that have failed and are eligible for retry
     */
    @Query("SELECT bp FROM BatchPayout bp WHERE bp.status = 'RETRY' AND bp.retryCount < bp.maxRetries")
    List<BatchPayout> findRetryablePayouts();

    /**
     * Check if batch has payout for vendor
     */
    boolean existsByBatchIdAndVendorId(Long batchId, Long vendorId);

    /**
     * Find payout by batch and vendor
     */
    Optional<BatchPayout> findByBatchIdAndVendorId(Long batchId, Long vendorId);
}

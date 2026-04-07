package com.farmeazy.repository;

import com.farmeazy.entity.PayoutApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutApprovalLogRepository extends JpaRepository<PayoutApprovalLog, Long> {

    /**
     * Get approval logs for a batch
     */
    List<PayoutApprovalLog> findByBatchIdOrderByApprovedAtDesc(Long batchId);

    /**
     * Check if approver has already approved this batch
     */
    boolean existsByBatchIdAndApprovedByUserId(Long batchId, Long userId);

    /**
     * Get last approval for a batch
     */
    Optional<PayoutApprovalLog> findFirstByBatchIdOrderByApprovedAtDesc(Long batchId);
}

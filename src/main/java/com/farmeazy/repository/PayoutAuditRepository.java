package com.farmeazy.repository;

import com.farmeazy.entity.PayoutAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutAuditRepository extends JpaRepository<PayoutAudit, Long> {

    /**
     * Get all audit entries for a batch
     */
    List<PayoutAudit> findByBatchIdOrderByTimestampDesc(Long batchId);

    /**
     * Get audit entries for a specific payout
     */
    @Query("SELECT a FROM PayoutAudit a WHERE a.batchPayout.id = ?1 ORDER BY a.timestamp DESC")
    List<PayoutAudit> findByPayoutIdOrderByTimestampDesc(Long payoutId);

    /**
     * Get all approvals for a batch
     */
    @Query("SELECT a FROM PayoutAudit a WHERE a.batch.id = ?1 AND a.action = 'APPROVED' ORDER BY a.timestamp DESC")
    List<PayoutAudit> findApprovalsByBatchId(Long batchId);

    /**
     * Get actions by specific user
     */
    List<PayoutAudit> findByActionByUserIdOrderByTimestampDesc(Long userId);
}

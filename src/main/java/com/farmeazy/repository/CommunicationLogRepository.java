package com.farmeazy.repository;

import com.farmeazy.entity.CommunicationLog;
import com.farmeazy.entity.CommunicationLog.CommunicationPurpose;
import com.farmeazy.entity.CommunicationLog.CommunicationStatus;
import com.farmeazy.entity.CommunicationLog.CommunicationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * COMMUNICATION LOG REPOSITORY
 * 
 * PURPOSE: Data access layer for communication audit trail.
 * Provides methods to query and analyze all notifications sent.
 */
@Repository
public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, Long> {

    /**
     * Find by user ID with pagination.
     */
    Page<CommunicationLog> findByRecipientUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find by type and status.
     */
    List<CommunicationLog> findByCommunicationTypeAndStatusOrderByCreatedAtDesc(
            CommunicationType type, CommunicationStatus status);

    /**
     * Find by purpose.
     */
    List<CommunicationLog> findByPurposeOrderByCreatedAtDesc(CommunicationPurpose purpose);

    /**
     * Find failed communications for retry.
     */
    @Query("SELECT c FROM CommunicationLog c WHERE c.status IN ('FAILED', 'BOUNCED') " +
           "AND c.retryCount < :maxRetries AND c.createdAt >= :since " +
           "ORDER BY c.retryCount ASC, c.createdAt ASC")
    List<CommunicationLog> findFailedForRetry(
            @Param("maxRetries") int maxRetries,
            @Param("since") LocalDateTime since);

    /**
     * Find by reference (e.g., order ID).
     */
    List<CommunicationLog> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
            String referenceType, Long referenceId);

    /**
     * Count by status for dashboard.
     */
    @Query("SELECT c.status, COUNT(c) FROM CommunicationLog c " +
           "WHERE c.createdAt >= :since GROUP BY c.status")
    List<Object[]> countByStatus(@Param("since") LocalDateTime since);

    /**
     * Count by purpose for analytics.
     */
    @Query("SELECT c.purpose, COUNT(c) FROM CommunicationLog c " +
           "WHERE c.createdAt >= :since GROUP BY c.purpose")
    List<Object[]> countByPurpose(@Param("since") LocalDateTime since);
}

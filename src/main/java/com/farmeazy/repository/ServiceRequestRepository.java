package com.farmeazy.repository;

import com.farmeazy.entity.ServiceRequest;
import com.farmeazy.entity.ServiceRequest.RequestCategory;
import com.farmeazy.entity.ServiceRequest.RequestPriority;
import com.farmeazy.entity.ServiceRequest.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SERVICE REQUEST REPOSITORY
 * 
 * PURPOSE: Data access layer for service request/support ticket management.
 * Provides methods to query and manage user support requests.
 */
@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    /**
     * Find by request number.
     */
    Optional<ServiceRequest> findByRequestNumber(String requestNumber);

    /**
     * Find requests by user ID.
     */
    Page<ServiceRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find requests by status.
     */
    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    /**
     * Find requests by category and status.
     */
    List<ServiceRequest> findByCategoryAndStatusOrderByPriorityAscCreatedAtAsc(
            RequestCategory category, RequestStatus status);

    /**
     * Find open high priority requests.
     */
    @Query("SELECT s FROM ServiceRequest s WHERE s.priority IN ('HIGH', 'CRITICAL') " +
           "AND s.status NOT IN ('RESOLVED', 'CLOSED') ORDER BY s.priority ASC, s.createdAt ASC")
    List<ServiceRequest> findOpenHighPriorityRequests();

    /**
     * Find requests pending support email.
     */
    List<ServiceRequest> findByEmailSentToSupportFalseOrderByCreatedAtAsc();

    /**
     * Find requests by related order.
     */
    List<ServiceRequest> findByRelatedOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * Count open requests by user.
     */
    @Query("SELECT COUNT(s) FROM ServiceRequest s WHERE s.user.id = :userId " +
           "AND s.status NOT IN ('RESOLVED', 'CLOSED')")
    long countOpenRequestsByUser(@Param("userId") Long userId);

    /**
     * Count requests by status for dashboard.
     */
    @Query("SELECT s.status, COUNT(s) FROM ServiceRequest s GROUP BY s.status")
    List<Object[]> countByStatus();

    /**
     * Find stale requests (open for more than X days).
     */
    @Query("SELECT s FROM ServiceRequest s WHERE s.status NOT IN ('RESOLVED', 'CLOSED') " +
           "AND s.createdAt < :cutoffDate ORDER BY s.createdAt ASC")
    List<ServiceRequest> findStaleRequests(@Param("cutoffDate") LocalDateTime cutoffDate);
}

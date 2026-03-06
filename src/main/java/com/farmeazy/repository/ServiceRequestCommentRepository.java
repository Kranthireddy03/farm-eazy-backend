package com.farmeazy.repository;

import com.farmeazy.entity.ServiceRequestComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SERVICE REQUEST COMMENT REPOSITORY
 * 
 * PURPOSE: Data access layer for service request comments/communication history.
 */
@Repository
public interface ServiceRequestCommentRepository extends JpaRepository<ServiceRequestComment, Long> {

    /**
     * Find comments by service request ID.
     */
    List<ServiceRequestComment> findByServiceRequestIdOrderByCreatedAtAsc(Long serviceRequestId);

    /**
     * Find public comments (exclude internal notes).
     */
    @Query("SELECT c FROM ServiceRequestComment c WHERE c.serviceRequest.id = :requestId " +
           "AND c.isInternalNote = false ORDER BY c.createdAt ASC")
    List<ServiceRequestComment> findPublicComments(@Param("requestId") Long serviceRequestId);

    /**
     * Find internal notes only.
     */
    @Query("SELECT c FROM ServiceRequestComment c WHERE c.serviceRequest.id = :requestId " +
           "AND c.isInternalNote = true ORDER BY c.createdAt ASC")
    List<ServiceRequestComment> findInternalNotes(@Param("requestId") Long serviceRequestId);

    /**
     * Count comments by service request.
     */
    long countByServiceRequestId(Long serviceRequestId);
}

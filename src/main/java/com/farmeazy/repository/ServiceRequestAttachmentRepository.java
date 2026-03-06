package com.farmeazy.repository;

import com.farmeazy.entity.ServiceRequestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SERVICE REQUEST ATTACHMENT REPOSITORY
 * 
 * PURPOSE: Data access layer for service request attachments.
 */
@Repository
public interface ServiceRequestAttachmentRepository extends JpaRepository<ServiceRequestAttachment, Long> {

    /**
     * Find attachments by service request ID.
     */
    List<ServiceRequestAttachment> findByServiceRequestIdOrderByCreatedAtDesc(Long serviceRequestId);

    /**
     * Count attachments by service request.
     */
    long countByServiceRequestId(Long serviceRequestId);

    /**
     * Find attachments by uploader.
     */
    List<ServiceRequestAttachment> findByUploadedByIdOrderByCreatedAtDesc(Long uploadedById);
}

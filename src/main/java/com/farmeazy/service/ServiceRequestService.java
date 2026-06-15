package com.farmeazy.service;

import com.farmeazy.dto.ServiceRequestDto;
import com.farmeazy.entity.*;
import com.farmeazy.entity.ServiceRequest.*;
import com.farmeazy.entity.ServiceRequestComment.CommentBy;
import com.farmeazy.entity.CommunicationLog.CommunicationPurpose;
import com.farmeazy.entity.CommunicationLog.CommunicationType;
import com.farmeazy.entity.CommunicationLog.CommunicationStatus;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.farmeazy.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * SERVICE REQUEST SERVICE
 * 
 * PURPOSE: Manages user support tickets and service requests.
 * Handles creation, updates, attachments, and email notifications.
 * 
 * KEY FEATURES:
 * - Creates service requests with unique 5-digit IDs
 * - Supports file attachments (documents, images)
 * - Sends notifications to no-reply@farm-eazy.com
 * - Tracks communication history
 * - Logs all actions for audit
 * 
 * EMAIL NOTIFICATIONS:
 * - New request → Support team (no-reply@farm-eazy.com)
 * - Status update → User
 * - Resolution → User
 */
@Service
public class ServiceRequestService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRequestService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ServiceRequestAttachmentRepository attachmentRepository;

    @Autowired
    private ServiceRequestCommentRepository commentRepository;

    @Autowired
    private CommunicationLogRepository communicationLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SequenceGeneratorService sequenceService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${farmeazy.support.email:no-reply@farm-eazy.com}")
    private String supportEmail;

    // ========== CREATE SERVICE REQUEST ==========

    /**
     * Creates a new service request from user.
     * Sends notification email to support team.
     */
    @Transactional
    public ServiceRequest createServiceRequest(Long userId, ServiceRequestDto dto) {
        logger.info("SERVICE_REQUEST_CREATE: userId={}, category={}", userId, dto.getCategory());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        // Generate unique request number
        String requestNumber = sequenceService.getNextServiceRequestId();
        
        ServiceRequest request = new ServiceRequest();
        request.setRequestNumber(requestNumber);
        request.setUser(user);
        request.setCategory(RequestCategory.valueOf(dto.getCategory()));
        request.setPriority(dto.getPriority() != null 
                ? RequestPriority.valueOf(dto.getPriority()) 
                : RequestPriority.MEDIUM);
        request.setSubject(dto.getSubject());
        request.setDescription(dto.getDescription());
        request.setRelatedOrderId(dto.getRelatedOrderId());
        request.setRelatedProductId(dto.getRelatedProductId());
        request.setUserEmail(user.getEmail());
        request.setUserPhone(user.getPhone());
        request.setStatus(RequestStatus.OPEN);
        
        request = serviceRequestRepository.save(request);
        
        logger.info("SERVICE_REQUEST_CREATED: requestNumber={}, userId={}, category={}",
                requestNumber, userId, dto.getCategory());
        auditLogger.info("SERVICE_REQUEST: action=CREATE, requestNumber={}, userId={}", 
                requestNumber, userId);
        
        // Send notification to support team
        sendSupportNotification(request);
        
        // Send confirmation to user
        sendUserConfirmation(request);
        
        return request;
    }

    /**
     * Adds attachment to service request.
     */
    @Transactional
    public ServiceRequestAttachment addAttachment(Long requestId, Long userId, MultipartFile file) 
            throws IOException {
        
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + requestId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1) 
                : "";
        
        String storedName = fileStorageService.store(file);
        String attachmentUrl = "/uploads/" + storedName;
        
        ServiceRequestAttachment attachment = new ServiceRequestAttachment();
        attachment.setServiceRequest(request);
        attachment.setFileName(storedName);
        attachment.setOriginalFileName(originalFilename);
        attachment.setFilePath(attachmentUrl);
        attachment.setFileType(extension);
        attachment.setFileSize(file.getSize());
        attachment.setMimeType(file.getContentType());
        attachment.setUploadedBy(user);
        
        attachment = attachmentRepository.save(attachment);
        
        logger.info("SERVICE_REQUEST_ATTACHMENT: requestNumber={}, fileName={}, size={}",
                request.getRequestNumber(), originalFilename, file.getSize());
        
        return attachment;
    }

    // ========== UPDATE SERVICE REQUEST ==========

    /**
     * Updates service request status.
     */
    @Transactional
    public ServiceRequest updateStatus(Long requestId, RequestStatus newStatus, String notes) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + requestId));
        
        RequestStatus oldStatus = request.getStatus();
        request.setStatus(newStatus);
        
        if (newStatus == RequestStatus.RESOLVED || newStatus == RequestStatus.CLOSED) {
            request.setResolutionDate(LocalDateTime.now());
            request.setResolutionNotes(notes);
        }
        
        request = serviceRequestRepository.save(request);
        
        // Add system comment
        addComment(requestId, null, CommentBy.SYSTEM, 
                "Status changed from " + oldStatus + " to " + newStatus + 
                (notes != null ? ". Notes: " + notes : ""), false);
        
        logger.info("SERVICE_REQUEST_STATUS_UPDATE: requestNumber={}, oldStatus={}, newStatus={}",
                request.getRequestNumber(), oldStatus, newStatus);
        
        // Notify user of status change
        sendStatusUpdateNotification(request, oldStatus, newStatus);
        
        return request;
    }

    /**
     * Adds a comment to service request.
     */
    @Transactional
    public ServiceRequestComment addComment(Long requestId, Long userId, CommentBy commentBy,
            String commentText, boolean isInternalNote) {
        
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + requestId));
        
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        
        ServiceRequestComment comment = new ServiceRequestComment();
        comment.setServiceRequest(request);
        comment.setUser(user);
        comment.setCommentBy(commentBy);
        comment.setCommentText(commentText);
        comment.setIsInternalNote(isInternalNote);
        
        comment = commentRepository.save(comment);
        
        logger.debug("SERVICE_REQUEST_COMMENT: requestNumber={}, commentBy={}, internal={}",
                request.getRequestNumber(), commentBy, isInternalNote);
        
        return comment;
    }

    // ========== QUERY METHODS ==========

    /**
     * Get service request by request number.
     */
    @Transactional(readOnly = true)
    public ServiceRequest getByRequestNumber(String requestNumber) {
        return serviceRequestRepository.findByRequestNumber(requestNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found: " + requestNumber));
    }

    /**
     * Get user's service requests with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ServiceRequest> getUserRequests(Long userId, Pageable pageable) {
        return serviceRequestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get open high priority requests for support dashboard.
     */
    @Transactional(readOnly = true)
    public List<ServiceRequest> getOpenHighPriorityRequests() {
        return serviceRequestRepository.findOpenHighPriorityRequests();
    }

    /**
     * Get request comments.
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestComment> getComments(Long requestId, boolean includeInternal) {
        if (includeInternal) {
            return commentRepository.findByServiceRequestIdOrderByCreatedAtAsc(requestId);
        }
        return commentRepository.findPublicComments(requestId);
    }

    /**
     * Get request attachments.
     */
    @Transactional(readOnly = true)
    public List<ServiceRequestAttachment> getAttachments(Long requestId) {
        return attachmentRepository.findByServiceRequestIdOrderByCreatedAtDesc(requestId);
    }

    // ========== EMAIL NOTIFICATIONS ==========

    /**
     * Sends notification to support team (no-reply@farm-eazy.com).
     */
    private void sendSupportNotification(ServiceRequest request) {
        try {
            emailService.sendServiceRequestSupportAlertEmail(
                    supportEmail,
                    request.getRequestNumber(),
                    request.getCategory() != null ? request.getCategory().name() : null,
                    request.getPriority() != null ? request.getPriority().name() : null,
                    request.getSubject(),
                    request.getUser().getUsername(),
                    request.getUserEmail(),
                    request.getUserPhone(),
                    request.getDescription(),
                    request.getRelatedOrderId() != null ? String.valueOf(request.getRelatedOrderId()) : null,
                    request.getCreatedAt()
            );
            
            request.setEmailSentToSupport(true);
            serviceRequestRepository.save(request);

            String subject = String.format("[%s] New Support Request: %s - %s",
                    request.getPriority(), request.getRequestNumber(), request.getSubject());
            
            // Log communication
            logCommunication(request, CommunicationType.EMAIL, CommunicationPurpose.SERVICE_REQUEST,
                    supportEmail, subject, CommunicationStatus.SENT);
            
            logger.info("SERVICE_REQUEST_EMAIL_SENT: requestNumber={}, to=support",
                    request.getRequestNumber());
            
        } catch (Exception e) {
            logger.error("SERVICE_REQUEST_EMAIL_FAILED: requestNumber={}, error={}",
                    request.getRequestNumber(), e.getMessage());
            
            logCommunication(request, CommunicationType.EMAIL, CommunicationPurpose.SERVICE_REQUEST,
                    supportEmail, "Support notification", CommunicationStatus.FAILED);
        }
    }

    /**
     * Sends confirmation email to user.
     */
    private void sendUserConfirmation(ServiceRequest request) {
        try {
            String serviceName = request.getCategory() != null
                    ? request.getCategory().name().replace("_", " ")
                    : "General Service";
            String status = request.getStatus() != null
                    ? request.getStatus().name().replace("_", " ")
                    : "OPEN";
            String serviceLocation = request.getRelatedOrderId() != null
                    ? "Related Order ID: " + request.getRelatedOrderId()
                    : request.getRelatedProductId() != null
                    ? "Related Product ID: " + request.getRelatedProductId()
                    : "Location details were not provided at submission.";

            emailService.sendServiceRequestConfirmationEmail(
                    request.getUserEmail(),
                    request.getUser().getUsername(),
                    request.getRequestNumber(),
                    serviceName,
                    request.getCreatedAt(),
                    "As per availability",
                    status,
                    serviceLocation,
                    request.getDescription()
            );

            request.setEmailNotificationSent(true);
            serviceRequestRepository.save(request);
            
            logCommunication(request, CommunicationType.EMAIL, CommunicationPurpose.SERVICE_REQUEST,
                    request.getUserEmail(),
                    "Service Request Confirmation - " + request.getRequestNumber(),
                    CommunicationStatus.SENT);
            
        } catch (Exception e) {
            logger.error("SERVICE_REQUEST_USER_EMAIL_FAILED: requestNumber={}, error={}",
                    request.getRequestNumber(), e.getMessage());
        }
    }

    /**
     * Sends status update notification to user.
     */
    private void sendStatusUpdateNotification(ServiceRequest request, RequestStatus oldStatus, 
            RequestStatus newStatus) {
        try {
            String subject = String.format("Service Request %s - Status Updated", request.getRequestNumber());

            emailService.sendServiceRequestStatusUpdateEmail(
                    request.getUserEmail(),
                    request.getUser().getUsername(),
                    request.getRequestNumber(),
                    oldStatus != null ? oldStatus.name() : null,
                    newStatus != null ? newStatus.name() : null,
                    request.getResolutionNotes()
            );
            
            logCommunication(request, CommunicationType.EMAIL, CommunicationPurpose.SERVICE_REQUEST,
                    request.getUserEmail(), subject, CommunicationStatus.SENT);
            
        } catch (Exception e) {
            logger.error("SERVICE_REQUEST_STATUS_EMAIL_FAILED: requestNumber={}, error={}",
                    request.getRequestNumber(), e.getMessage());
        }
    }

    /**
     * Logs communication for audit trail.
     */
    private void logCommunication(ServiceRequest request, CommunicationType type, 
            CommunicationPurpose purpose, String recipient, String subject, CommunicationStatus status) {
        
        CommunicationLog log = new CommunicationLog();
        log.setCommunicationType(type);
        log.setPurpose(purpose);
        log.setRecipientUser(request.getUser());
        log.setRecipientEmail(recipient);
        log.setSubject(subject);
        log.setContentSummary("Service request: " + request.getRequestNumber());
        log.setReferenceType("SERVICE_REQUEST");
        log.setReferenceId(request.getId());
        log.setStatus(status);
        log.setProvider("EmailService");
        log.setSentAt(LocalDateTime.now());
        
        communicationLogRepository.save(log);
    }
}

package com.farmeazy.controller;

import com.farmeazy.dto.ServiceRequestDto;
import com.farmeazy.dto.ServiceRequestResponseDto;
import com.farmeazy.entity.ServiceRequest;
import com.farmeazy.entity.ServiceRequestAttachment;
import com.farmeazy.entity.ServiceRequestComment;
import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SERVICE REQUEST CONTROLLER
 * 
 * PURPOSE: REST API for user support tickets and service requests.
 * Allows users to raise issues, upload attachments, and track status.
 * 
 * ENDPOINTS:
 * - POST /api/service-requests         - Create new service request
 * - GET /api/service-requests          - Get user's requests (paginated)
 * - GET /api/service-requests/{id}     - Get specific request details
 * - POST /api/service-requests/{id}/attachments - Upload attachment
 * - POST /api/service-requests/{id}/comments - Add comment
 * - GET /api/service-requests/{id}/comments - Get comments
 * 
 * WHY THIS API EXISTS:
 * Users need a way to raise issues they're facing (payment problems,
 * delivery issues, technical bugs, etc.). This provides a structured
 * way to submit and track support requests with email notifications
 * to the support team at no-reply@farm-eazy.com.
 */
@RestController
@RequestMapping("/api/service-requests")
@Tag(name = "Service Requests", description = "APIs for user support tickets and service requests")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class ServiceRequestController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRequestController.class);

    @Autowired
    private ServiceRequestService serviceRequestService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Creates a new service request.
     * 
     * WHY: Allows users to raise issues about payments, orders, delivery,
     * technical problems, etc. The request is logged and an email is
     * sent to the support team for review.
     * 
     * @param dto Request details including category, subject, description
     * @param auth Current authenticated user
     * @return Created service request with unique request number
     */
    @PostMapping
    @Operation(summary = "Create service request", 
               description = "Create a new support ticket. Request is sent to support team via email.")
    public ResponseEntity<?> createServiceRequest(
            @Valid @RequestBody ServiceRequestDto dto,
            Authentication auth) {
        
        logger.info("SERVICE_REQUEST_API: Creating request for user={}", auth.getName());
        
        User user = getUserFromAuth(auth);
        ServiceRequest request = serviceRequestService.createServiceRequest(user.getId(), dto);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Service request created successfully",
                "requestNumber", request.getRequestNumber(),
                "id", request.getId()
        ));
    }

    /**
     * Gets user's service requests with pagination.
     * 
     * WHY: Users need to track their submitted requests and see status updates.
     */
    @GetMapping
    @Operation(summary = "Get user's requests", 
               description = "Get all service requests for the authenticated user with pagination")
    public ResponseEntity<?> getUserRequests(
            Authentication auth,
            Pageable pageable) {
        
        User user = getUserFromAuth(auth);
        Page<ServiceRequest> requests = serviceRequestService.getUserRequests(user.getId(), pageable);
        
        Page<ServiceRequestResponseDto> dtoPage = requests.map(this::toResponseDto);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Gets details of a specific service request.
     * 
     * WHY: Users need to view full details including comments and attachments.
     */
    @GetMapping("/{requestNumber}")
    @Operation(summary = "Get request details", 
               description = "Get detailed information about a specific service request")
    public ResponseEntity<?> getRequestDetails(
            @PathVariable String requestNumber,
            Authentication auth) {
        
        ServiceRequest request = serviceRequestService.getByRequestNumber(requestNumber);
        
        // Verify user owns this request
        User user = getUserFromAuth(auth);
        if (!request.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied",
                    "message", "You can only view your own service requests"
            ));
        }
        
        ServiceRequestResponseDto dto = toDetailedResponseDto(request);
        return ResponseEntity.ok(dto);
    }

    /**
     * Uploads attachment to service request.
     * 
     * WHY: Users may need to provide screenshots, documents, or other files
     * to help explain their issue.
     */
    @PostMapping(value = "/{requestNumber}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload attachment", 
               description = "Upload a file attachment to an existing service request")
    public ResponseEntity<?> uploadAttachment(
            @PathVariable String requestNumber,
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {
        
        ServiceRequest request = serviceRequestService.getByRequestNumber(requestNumber);
        User user = getUserFromAuth(auth);
        
        // Verify ownership
        if (!request.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied"
            ));
        }
        
        logger.info("SERVICE_REQUEST_ATTACHMENT: requestNumber={}, file={}",
                requestNumber, file.getOriginalFilename());
        
        ServiceRequestAttachment attachment = serviceRequestService.addAttachment(
                request.getId(), user.getId(), file);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "attachmentId", attachment.getId(),
                "fileName", attachment.getOriginalFileName()
        ));
    }

    /**
     * Adds a comment to service request.
     * 
     * WHY: Enables communication between user and support team.
     */
    @PostMapping("/{requestNumber}/comments")
    @Operation(summary = "Add comment", 
               description = "Add a comment to an existing service request")
    public ResponseEntity<?> addComment(
            @PathVariable String requestNumber,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        
        ServiceRequest request = serviceRequestService.getByRequestNumber(requestNumber);
        User user = getUserFromAuth(auth);
        
        // Verify ownership
        if (!request.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied"
            ));
        }
        
        String commentText = body.get("comment");
        if (commentText == null || commentText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Comment text is required"
            ));
        }
        
        ServiceRequestComment comment = serviceRequestService.addComment(
                request.getId(), user.getId(), 
                ServiceRequestComment.CommentBy.USER, 
                commentText, false);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "commentId", comment.getId()
        ));
    }

    /**
     * Gets comments for a service request.
     */
    @GetMapping("/{requestNumber}/comments")
    @Operation(summary = "Get comments", 
               description = "Get all comments for a service request")
    public ResponseEntity<?> getComments(
            @PathVariable String requestNumber,
            Authentication auth) {
        
        ServiceRequest request = serviceRequestService.getByRequestNumber(requestNumber);
        User user = getUserFromAuth(auth);
        
        // Verify ownership
        if (!request.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }
        
        List<ServiceRequestComment> comments = serviceRequestService.getComments(
                request.getId(), false);
        
        List<ServiceRequestResponseDto.CommentInfo> commentDtos = comments.stream()
                .map(c -> new ServiceRequestResponseDto.CommentInfo(
                        c.getId(),
                        c.getCommentBy().name(),
                        c.getCommentText(),
                        c.getCreatedAt()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(commentDtos);
    }

    // ========== HELPER METHODS ==========

    private User getUserFromAuth(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ServiceRequestResponseDto toResponseDto(ServiceRequest request) {
        ServiceRequestResponseDto dto = new ServiceRequestResponseDto();
        dto.setId(request.getId());
        dto.setRequestNumber(request.getRequestNumber());
        dto.setCategory(request.getCategory().name());
        dto.setPriority(request.getPriority().name());
        dto.setSubject(request.getSubject());
        dto.setStatus(request.getStatus().name());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        return dto;
    }

    private ServiceRequestResponseDto toDetailedResponseDto(ServiceRequest request) {
        ServiceRequestResponseDto dto = toResponseDto(request);
        dto.setDescription(request.getDescription());
        dto.setRelatedOrderId(request.getRelatedOrderId());
        dto.setRelatedProductId(request.getRelatedProductId());
        dto.setAssignedTo(request.getAssignedTo());
        dto.setResolutionNotes(request.getResolutionNotes());
        dto.setResolutionDate(request.getResolutionDate());
        
        // Add attachments
        List<ServiceRequestAttachment> attachments = serviceRequestService.getAttachments(request.getId());
        dto.setAttachmentCount(attachments.size());
        dto.setAttachments(attachments.stream()
                .map(a -> new ServiceRequestResponseDto.AttachmentInfo(
                        a.getId(),
                        a.getFileName(),
                        a.getOriginalFileName(),
                        a.getFileType(),
                        a.getFileSize(),
                        a.getCreatedAt()
                ))
                .collect(Collectors.toList()));
        
        // Add comments
        List<ServiceRequestComment> comments = serviceRequestService.getComments(request.getId(), false);
        dto.setCommentCount(comments.size());
        dto.setComments(comments.stream()
                .map(c -> new ServiceRequestResponseDto.CommentInfo(
                        c.getId(),
                        c.getCommentBy().name(),
                        c.getCommentText(),
                        c.getCreatedAt()
                ))
                .collect(Collectors.toList()));
        
        return dto;
    }
}

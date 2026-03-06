package com.farmeazy.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SERVICE REQUEST RESPONSE DTO
 * 
 * Used for returning service request details to client.
 */
public class ServiceRequestResponseDto {

    private Long id;
    private String requestNumber;
    private String category;
    private String priority;
    private String subject;
    private String description;
    private String status;
    private Long relatedOrderId;
    private Long relatedProductId;
    private String assignedTo;
    private String resolutionNotes;
    private LocalDateTime resolutionDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int attachmentCount;
    private int commentCount;
    private List<AttachmentInfo> attachments;
    private List<CommentInfo> comments;

    // No-args constructor
    public ServiceRequestResponseDto() {
    }

    // All-args constructor
    public ServiceRequestResponseDto(Long id, String requestNumber, String category, String priority,
                                      String subject, String description, String status,
                                      Long relatedOrderId, Long relatedProductId, String assignedTo,
                                      String resolutionNotes, LocalDateTime resolutionDate,
                                      LocalDateTime createdAt, LocalDateTime updatedAt,
                                      int attachmentCount, int commentCount,
                                      List<AttachmentInfo> attachments, List<CommentInfo> comments) {
        this.id = id;
        this.requestNumber = requestNumber;
        this.category = category;
        this.priority = priority;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.relatedOrderId = relatedOrderId;
        this.relatedProductId = relatedProductId;
        this.assignedTo = assignedTo;
        this.resolutionNotes = resolutionNotes;
        this.resolutionDate = resolutionDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.attachmentCount = attachmentCount;
        this.commentCount = commentCount;
        this.attachments = attachments;
        this.comments = comments;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getRelatedOrderId() {
        return relatedOrderId;
    }

    public void setRelatedOrderId(Long relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }

    public Long getRelatedProductId() {
        return relatedProductId;
    }

    public void setRelatedProductId(Long relatedProductId) {
        this.relatedProductId = relatedProductId;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public LocalDateTime getResolutionDate() {
        return resolutionDate;
    }

    public void setResolutionDate(LocalDateTime resolutionDate) {
        this.resolutionDate = resolutionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getAttachmentCount() {
        return attachmentCount;
    }

    public void setAttachmentCount(int attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public List<AttachmentInfo> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentInfo> attachments) {
        this.attachments = attachments;
    }

    public List<CommentInfo> getComments() {
        return comments;
    }

    public void setComments(List<CommentInfo> comments) {
        this.comments = comments;
    }

    public static class AttachmentInfo {
        private Long id;
        private String fileName;
        private String originalFileName;
        private String fileType;
        private Long fileSize;
        private LocalDateTime createdAt;

        // No-args constructor
        public AttachmentInfo() {
        }

        // All-args constructor
        public AttachmentInfo(Long id, String fileName, String originalFileName, String fileType,
                               Long fileSize, LocalDateTime createdAt) {
            this.id = id;
            this.fileName = fileName;
            this.originalFileName = originalFileName;
            this.fileType = fileType;
            this.fileSize = fileSize;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class CommentInfo {
        private Long id;
        private String commentBy;
        private String commentText;
        private LocalDateTime createdAt;

        // No-args constructor
        public CommentInfo() {
        }

        // All-args constructor
        public CommentInfo(Long id, String commentBy, String commentText, LocalDateTime createdAt) {
            this.id = id;
            this.commentBy = commentBy;
            this.commentText = commentText;
            this.createdAt = createdAt;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCommentBy() {
            return commentBy;
        }

        public void setCommentBy(String commentBy) {
            this.commentBy = commentBy;
        }

        public String getCommentText() {
            return commentText;
        }

        public void setCommentText(String commentText) {
            this.commentText = commentText;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}

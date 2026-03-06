package com.farmeazy.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * SERVICE REQUEST COMMENT ENTITY
 * 
 * PURPOSE: Tracks communication history on service requests.
 * Stores comments from users, support staff, and system-generated messages.
 * 
 * KEY FEATURES:
 * - Links to parent service request
 * - Identifies comment source (user, support, system)
 * - Supports internal notes for support staff
 */
@Entity
@Table(name = "service_request_comment")
public class ServiceRequestComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_by", nullable = false)
    private CommentBy commentBy;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "is_internal_note")
    private Boolean isInternalNote = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // No-args constructor
    public ServiceRequestComment() {
    }

    // All-args constructor
    public ServiceRequestComment(Long id, ServiceRequest serviceRequest, User user,
                                  CommentBy commentBy, String commentText, Boolean isInternalNote,
                                  LocalDateTime createdAt) {
        this.id = id;
        this.serviceRequest = serviceRequest;
        this.user = user;
        this.commentBy = commentBy;
        this.commentText = commentText;
        this.isInternalNote = isInternalNote;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(ServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public CommentBy getCommentBy() {
        return commentBy;
    }

    public void setCommentBy(CommentBy commentBy) {
        this.commentBy = commentBy;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public Boolean getIsInternalNote() {
        return isInternalNote;
    }

    public void setIsInternalNote(Boolean isInternalNote) {
        this.isInternalNote = isInternalNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Comment By Enum
    public enum CommentBy {
        USER,
        SUPPORT,
        SYSTEM
    }
}

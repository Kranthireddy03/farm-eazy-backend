package com.farmeazy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FAQQuestionDto {
        // Source of the question (e.g., FAQ_PUBLIC_PAGE, ADMIN, etc.)
        private String source;

    // Publish target for approved FAQ: USER, ADMIN, BOTH
    private String visibilityTarget;

        public String getSource() {
            return source;
        }
        public void setSource(String source) {
            this.source = source;
        }

        public String getVisibilityTarget() {
            return visibilityTarget;
        }

        public void setVisibilityTarget(String visibilityTarget) {
            this.visibilityTarget = visibilityTarget;
        }
    @NotBlank(message = "Question is required")
    @Size(min = 10, max = 1000, message = "Question must be between 10 and 1000 characters")
    private String question;

    // Optional extra details provided by users in separate form fields.
    private String details;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;
    // For users, userId will be set; for non-users, it will be null
    private String userId;
    // Answer (optional) when question is published to FAQ
    private String answer;
    private Boolean addedToFAQ;
    private java.time.OffsetDateTime answeredAt;
    // Database id
    private Long id;
    private java.time.OffsetDateTime submittedAt;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public java.time.OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(java.time.OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Boolean getAddedToFAQ() {
        return addedToFAQ;
    }

    public void setAddedToFAQ(Boolean addedToFAQ) {
        this.addedToFAQ = addedToFAQ;
    }

    public java.time.OffsetDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(java.time.OffsetDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }

    // notificationRead field to match entity
    private Boolean notificationRead;

    // Conversation history for public detail view
    private java.util.List<FAQCommunicationDto> communications;

    // Computed workflow status for admin queue handling: PENDING, RESOLVED, REJECTED
    private String workflowStatus;

    public Boolean isNotificationRead() {
        return notificationRead;
    }

    public void setNotificationRead(Boolean notificationRead) {
        this.notificationRead = notificationRead;
    }

    public java.util.List<FAQCommunicationDto> getCommunications() {
        return communications;
    }

    public void setCommunications(java.util.List<FAQCommunicationDto> communications) {
        this.communications = communications;
    }

    public String getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(String workflowStatus) {
        this.workflowStatus = workflowStatus;
    }
}

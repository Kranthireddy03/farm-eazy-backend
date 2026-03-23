// Removed Lombok
package com.farmeazy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Entity
// ...existing code...
public class FAQQuestion {
        // Source of the question (e.g., FAQ_PUBLIC_PAGE, ADMIN, etc.)
        private String source;

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 1000)
    private String question;
    @Column(nullable = false)
    private String email;
    private String userId;
    @Column(columnDefinition = "TEXT")
    private String answer;
    private boolean addedToFAQ = false;
    // whether admin notification for this question has been marked read
    @Column(name = "notification_read", nullable = false)
    private Boolean notificationRead = false;
    private OffsetDateTime submittedAt;
    private OffsetDateTime answeredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public boolean isAddedToFAQ() { return addedToFAQ; }
    public void setAddedToFAQ(boolean addedToFAQ) { this.addedToFAQ = addedToFAQ; }
    public Boolean isNotificationRead() { return notificationRead; }
    public void setNotificationRead(Boolean notificationRead) { this.notificationRead = notificationRead; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
    public OffsetDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(OffsetDateTime answeredAt) { this.answeredAt = answeredAt; }
}

// Removed Lombok
package com.farmeazy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
// ...existing code...
public class FAQQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 1000)
    private String question;
    @Column(nullable = false)
    private String email;
    private String userId;
    private String answer;
    private boolean addedToFAQ = false;
    // whether admin notification for this question has been marked read
    @Column(name = "notification_read", insertable = false, updatable = false)
    private boolean notificationRead = false;
    private LocalDateTime submittedAt;
    private LocalDateTime answeredAt;

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
    public boolean isNotificationRead() { return notificationRead; }
    public void setNotificationRead(boolean notificationRead) { this.notificationRead = notificationRead; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}

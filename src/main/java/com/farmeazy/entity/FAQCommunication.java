// Removed Lombok
package com.farmeazy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class FAQCommunication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "faq_question_id")
    private FAQQuestion faqQuestion;
    @Column(nullable = false)
    private String recipientEmail;
    @Column(nullable = false)
    private String subject;
    @Column(nullable = false, length = 4000)
    private String body;
    @Column(nullable = false)
    private String purpose; // e.g. "FAQ Addition Notification", "Answer Notification"
    private LocalDateTime sentAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FAQQuestion getFaqQuestion() { return faqQuestion; }
    public void setFaqQuestion(FAQQuestion faqQuestion) { this.faqQuestion = faqQuestion; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}

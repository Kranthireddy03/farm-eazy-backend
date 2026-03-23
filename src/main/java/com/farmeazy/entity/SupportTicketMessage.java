package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_ticket_messages")
public class SupportTicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "support_ticket_id", nullable = false)
    private Long supportTicketId;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType;

    @Column(name = "sender_name", length = 255)
    private String senderName;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "attachment_url", length = 2048)
    private String attachmentUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public SupportTicketMessage() {}

    public SupportTicketMessage(Long supportTicketId, String senderType, String senderName, String message, String attachmentUrl) {
        this.supportTicketId = supportTicketId;
        this.senderType = senderType;
        this.senderName = senderName;
        this.message = message;
        this.attachmentUrl = attachmentUrl;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSupportTicketId() { return supportTicketId; }
    public void setSupportTicketId(Long supportTicketId) { this.supportTicketId = supportTicketId; }

    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

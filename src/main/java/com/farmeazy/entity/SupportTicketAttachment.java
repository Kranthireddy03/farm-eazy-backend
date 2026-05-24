package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_ticket_attachments")
public class SupportTicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "support_ticket_message_id", nullable = false)
    private Long supportTicketMessageId;

    @Column(name = "file_name", length = 512)
    private String fileName;

    @Column(name = "url", length = 2048)
    private String url;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public SupportTicketAttachment() {}

    public SupportTicketAttachment(Long supportTicketMessageId, String fileName, String url) {
        this.supportTicketMessageId = supportTicketMessageId;
        this.fileName = fileName;
        this.url = url;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSupportTicketMessageId() { return supportTicketMessageId; }
    public void setSupportTicketMessageId(Long supportTicketMessageId) { this.supportTicketMessageId = supportTicketMessageId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

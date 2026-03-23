package com.farmeazy.dto;

import com.farmeazy.entity.SupportTicketMessage;

import java.time.LocalDateTime;

public class SupportTicketMessageDto {
    private Long id;
    private String senderType;
    private String senderName;
    private String message;
    private String attachmentUrl;
    private LocalDateTime createdAt;

    public static SupportTicketMessageDto fromEntity(SupportTicketMessage msg) {
        SupportTicketMessageDto dto = new SupportTicketMessageDto();
        dto.setId(msg.getId());
        dto.setSenderType(msg.getSenderType());
        dto.setSenderName(msg.getSenderName());
        dto.setMessage(msg.getMessage());
        dto.setAttachmentUrl(msg.getAttachmentUrl());
        dto.setCreatedAt(msg.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

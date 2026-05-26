package com.farmeazy.dto;

import com.farmeazy.entity.SupportTicketMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.farmeazy.entity.SupportTicketAttachment;
import com.farmeazy.dto.SupportTicketAttachmentDto;

public class SupportTicketMessageDto {

    private Long id;
    private String senderType;
    private String senderName;
    private String message;
    private LocalDateTime createdAt;
    private List<SupportTicketAttachmentDto> attachments;

    public static SupportTicketMessageDto fromEntity(SupportTicketMessage msg) {
        SupportTicketMessageDto dto = new SupportTicketMessageDto();

        dto.setId(msg.getId());
        dto.setSenderType(msg.getSenderType());
        dto.setSenderName(msg.getSenderName());
        dto.setMessage(msg.getMessage());
        dto.setCreatedAt(msg.getCreatedAt());
        dto.setAttachments(null);
        return dto;
    }

    public List<SupportTicketAttachmentDto> getAttachments() { return attachments; }
    public void setAttachments(List<SupportTicketAttachmentDto> attachments) { this.attachments = attachments; }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

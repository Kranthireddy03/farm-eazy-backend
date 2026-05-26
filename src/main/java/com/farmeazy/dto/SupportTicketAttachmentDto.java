package com.farmeazy.dto;

import com.farmeazy.entity.SupportTicketAttachment;
import java.time.LocalDateTime;

public class SupportTicketAttachmentDto {
    private Long id;
    private String fileName;
    private String url;
    private LocalDateTime createdAt;

    public static SupportTicketAttachmentDto fromEntity(SupportTicketAttachment a) {
        if (a == null) return null;
        SupportTicketAttachmentDto d = new SupportTicketAttachmentDto();
        d.setId(a.getId());
        d.setFileName(a.getFileName());
        d.setUrl(a.getUrl());
        d.setCreatedAt(a.getCreatedAt());
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

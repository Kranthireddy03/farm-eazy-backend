package com.farmeazy.dto;

// Removed Lombok. Manual getters, setters, and constructors below.

public class TicketDto {
    private Long id;
    private String subject;
    private String status;
    private java.time.OffsetDateTime createdAt;
    private String source;
    private String priority;

    public TicketDto() {}

    public TicketDto(Long id, String subject, String status, java.time.OffsetDateTime createdAt) {
        this.id = id;
        this.subject = subject;
        this.status = status;
        this.createdAt = createdAt;
    }

    public TicketDto(Long id, String subject, String status, java.time.OffsetDateTime createdAt, String source) {
        this(id, subject, status, createdAt);
        this.source = source;
    }

    public TicketDto(Long id, String subject, String status, java.time.OffsetDateTime createdAt, String source, String priority) {
        this(id, subject, status, createdAt, source);
        this.priority = priority;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public java.time.OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}

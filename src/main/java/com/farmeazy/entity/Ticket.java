package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import com.farmeazy.model.TicketPriority;
import com.farmeazy.model.TicketStatus;
import com.farmeazy.model.TicketCategory;
import java.time.OffsetDateTime;
import com.farmeazy.model.TicketPriority;
import com.farmeazy.model.TicketStatus;
import com.farmeazy.model.TicketCategory;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_id", unique = true, nullable = false)
    private String displayId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    private String module;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "is_important")
    private Boolean important = false;

    @Column(name = "is_archived")
    private Boolean archived = false;

    @Column(name = "sla_by")
    private OffsetDateTime slaBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Ticket() {}

    public Ticket(Long id, String displayId, String title, String description, TicketCategory category, TicketPriority priority, TicketStatus status, String module, Long createdBy, Long assignedTo, Boolean important, Boolean archived, OffsetDateTime slaBy, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.displayId = displayId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.module = module;
        this.createdBy = createdBy;
        this.assignedTo = assignedTo;
        this.important = important;
        this.archived = archived;
        this.slaBy = slaBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDisplayId() { return displayId; }
    public void setDisplayId(String displayId) { this.displayId = displayId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TicketCategory getCategory() { return category; }
    public void setCategory(TicketCategory category) { this.category = category; }
    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public Boolean getImportant() { return important; }
    public void setImportant(Boolean important) { this.important = important; }
    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }
    public OffsetDateTime getSlaBy() { return slaBy; }
    public void setSlaBy(OffsetDateTime slaBy) { this.slaBy = slaBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

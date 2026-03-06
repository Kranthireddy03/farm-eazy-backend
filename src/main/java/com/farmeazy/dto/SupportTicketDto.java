package com.farmeazy.dto;

import com.farmeazy.entity.SupportTicket.TicketCategory;
import com.farmeazy.entity.SupportTicket.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SUPPORT TICKET DTO
 * 
 * PURPOSE: Transfer object for creating support tickets.
 */
public class SupportTicketDto {

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private TicketCategory category = TicketCategory.GENERAL;

    private TicketPriority priority = TicketPriority.MEDIUM;

    private String contactEmail;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String contactPhone;

    private Long orderId;

    private Long serviceId;

    // Constructors
    public SupportTicketDto() {}

    // Getters and Setters
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketCategory getCategory() { return category; }
    public void setCategory(TicketCategory category) { this.category = category; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
}

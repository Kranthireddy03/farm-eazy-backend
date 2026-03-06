package com.farmeazy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SERVICE REQUEST DTO
 * 
 * Used for creating new service requests from users.
 */
public class ServiceRequestDto {

    @NotBlank(message = "Category is required")
    private String category;

    private String priority;

    @NotBlank(message = "Subject is required")
    @Size(min = 5, max = 200, message = "Subject must be between 5 and 200 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    private String description;

    private Long relatedOrderId;

    private Long relatedProductId;

    // No-args constructor
    public ServiceRequestDto() {
    }

    // All-args constructor
    public ServiceRequestDto(String category, String priority, String subject, String description,
                              Long relatedOrderId, Long relatedProductId) {
        this.category = category;
        this.priority = priority;
        this.subject = subject;
        this.description = description;
        this.relatedOrderId = relatedOrderId;
        this.relatedProductId = relatedProductId;
    }

    // Getters and Setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getRelatedOrderId() {
        return relatedOrderId;
    }

    public void setRelatedOrderId(Long relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }

    public Long getRelatedProductId() {
        return relatedProductId;
    }

    public void setRelatedProductId(Long relatedProductId) {
        this.relatedProductId = relatedProductId;
    }
}

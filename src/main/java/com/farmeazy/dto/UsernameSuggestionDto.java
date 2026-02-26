package com.farmeazy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * USERNAME SUGGESTION DTO (Data Transfer Object)
 * 
 * PURPOSE: Transfers email and phone data for generating username suggestions.
 * Used by /api/auth/suggest-username endpoint.
 * 
 * WORKFLOW:
 * 1. User provides email and phone during registration
 * 2. Frontend calls /api/auth/suggest-username with this DTO
 * 3. Backend generates 3-5 available username options
 * 4. User selects preferred username or uses auto-generated one
 */
public class UsernameSuggestionDto {
    
    /**
     * USER EMAIL
     * Required for extracting username prefix
     * Example: "john@example.com" → "john"
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    /**
     * PHONE NUMBER
     * Required for generating phone-based username suffix
     * Example: "9876543210" → last 4 digits "3210"
     */
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;
    
    public UsernameSuggestionDto() {}
    
    public UsernameSuggestionDto(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
}

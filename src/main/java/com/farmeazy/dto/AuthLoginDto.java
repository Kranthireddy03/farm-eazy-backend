package com.farmeazy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AUTH LOGIN DTO (Data Transfer Object)
 * 
 * PURPOSE: Validates and transfers user login credentials from client to server.
 * Acts as the request contract for the login API endpoint (/api/auth/login).
 * 
 * KEY FEATURES:
 * 1. Authentication: Collects email/username and password for user verification
 * 2. Validation: Ensures identifier and password are provided
 * 3. Security: Validates input to prevent injection attacks
 * 
 * HOW IT WORKS:
 * - Client sends login request with JSON body mapped to this DTO
 * - User can login with either email OR username
 * - Spring validates @NotBlank annotations
 * - If validation fails, error response is returned with details
 * - If validation passes, credentials are verified against database
 * - On success: User and JWT token are returned in AuthResponseDto
 * - On failure: Unauthorized exception is thrown
 * 
 * VALIDATION RULES:
 * - email: Required (can be email or username)
 * - password: Required, minimum 6 characters (user's registered password)
 * 
 * USAGE EXAMPLES:
 * POST /api/auth/login (with email)
 * {
 *     "email": "rajesh@example.com",
 *     "password": "SecurePass123"
 * }
 * 
 * POST /api/auth/login (with username)
 * {
 *     "email": "rajesh_farmer",
 *     "password": "SecurePass123"
 * }
 * 
 * RESPONSE (on success):
 * {
 *     "id": 10001,
 *     "email": "rajesh@example.com",
 *     "username": "rajesh_farmer",
 *     "roles": ["USER"],
 *     "token": "eyJhbGciOiJIUzUxMiJ9...",
 *     "tokenType": "Bearer"
 * }
 */
public class AuthLoginDto {
    
    /**
     * IDENTIFIER - USER'S LOGIN IDENTIFIER
     * @NotBlank: Field cannot be null or empty
     * Can be any of: email, username, or user ID (5-digit number)
     * Example: "rajesh@example.com" or "rajesh_farmer" or "10001"
     */
    @NotBlank(message = "Email, username, or user ID is required")
    private String identifier;
    
    /**
     * PASSWORD - USER'S LOGIN CREDENTIAL
     * @NotBlank: Field cannot be null or empty
     * @Size(min = 6): Minimum 6 characters (should match registration requirement)
     * This password must match the one registered by the user
     * Password is compared with encrypted password in database
     * Example: "SecurePass123"
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /**
     * Remember-me toggle from client.
     * true: issue refresh token for long-lived sign-in.
     * false: session-only auth with short-lived access token.
     */
    private Boolean rememberMe = false;
    
    public AuthLoginDto() {}
    
    public AuthLoginDto(String identifier, String password) {
        this.identifier = identifier;
        this.password = password;
    }
    
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    
    // Alias for backward compatibility - maps to identifier
    public String getEmail() { return identifier; }
    public void setEmail(String email) { this.identifier = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getRememberMe() { return rememberMe; }
    public void setRememberMe(Boolean rememberMe) { this.rememberMe = rememberMe; }
}

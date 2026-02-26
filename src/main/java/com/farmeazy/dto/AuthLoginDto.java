package com.farmeazy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AUTH LOGIN DTO (Data Transfer Object)
 * 
 * PURPOSE: Validates and transfers user login credentials from client to server.
 * Acts as the request contract for the login API endpoint (/api/auth/login).
 * 
 * KEY FEATURES:
 * 1. Authentication: Collects email and password for user verification
 * 2. Validation: Ensures email format and password strength
 * 3. Security: Validates input to prevent injection attacks
 * 
 * HOW IT WORKS:
 * - Client sends login request with JSON body mapped to this DTO
 * - Spring validates @NotBlank and @Email annotations
 * - If validation fails, error response is returned with details
 * - If validation passes, credentials are verified against database
 * - On success: User and JWT token are returned in AuthResponseDto
 * - On failure: Unauthorized exception is thrown
 * 
 * VALIDATION RULES:
 * - email: Required, must be valid email format (user's registered email)
 * - password: Required, minimum 6 characters (user's registered password)
 * 
 * USAGE EXAMPLE:
 * POST /api/auth/login
 * {
 *     "email": "rajesh@example.com",
 *     "password": "SecurePass123"
 * }
 * 
 * RESPONSE (on success):
 * {
 *     "id": 1,
 *     "email": "rajesh@example.com",
 *     "fullName": "Rajesh Kumar",
 *     "roles": ["USER"],
 *     "token": "eyJhbGciOiJIUzUxMiJ9...",
 *     "tokenType": "Bearer"
 * }
 */
public class AuthLoginDto {
    
    /**
     * EMAIL - USER'S LOGIN IDENTIFIER
     * @NotBlank: Field cannot be null or empty
     * @Email: Must be valid email format
     * This email must match the one used during registration
     * Example: "rajesh@example.com"
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
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
    
    public AuthLoginDto() {}
    
    public AuthLoginDto(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

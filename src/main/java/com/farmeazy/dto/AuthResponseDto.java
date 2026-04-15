package com.farmeazy.dto;

import java.util.Set;

/**
 * AUTH RESPONSE DTO (Data Transfer Object)
 * 
 * PURPOSE: Sends authenticated user information and JWT token to the client after successful login/registration.
 * Acts as the response contract for both /api/auth/login and /api/auth/register endpoints.
 * 
 * KEY INFORMATION PROVIDED:
 * 1. User Details: id, email, fullName for client-side user identification
 * 2. Authorization: roles for client-side permission checks and features display
 * 3. JWT Token: token for authenticating subsequent API requests
 * 4. Token Type: "Bearer" indicates how to use token in Authorization header
 * 
 * HOW IT WORKS:
 * - After successful authentication, server creates this response
 * - Includes all user info needed by frontend application
 * - JWT token is returned for stateless authentication
 * - Client stores token and includes it in Authorization header of future requests
 * - Server validates token to confirm user identity and permissions
 * 
 * USAGE ON CLIENT:
 * - Store token: localStorage.setItem('authToken', response.token)
 * - Include in requests: Authorization: "Bearer <token>"
 * - Display user: show fullName in UI, use roles for permission checks
 * 
 * RESPONSE EXAMPLE:
 * {
 *     "id": 1,
 *     "email": "rajesh@example.com",
 *     "fullName": "Rajesh Kumar",
 *     "roles": ["USER"],
 *     "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6WyJST0xFX1VTRVIiXSwiZW1haWwiOiJyYWplc2hAZXhhbXBsZS5jb20iLCJpYXQiOjE2NjM5NTQzODcsImV4cCI6MTY2NDAzODM4N30...",
 *     "tokenType": "Bearer"
 * }
 */
public class AuthResponseDto {
    /**
     * USER ID - UNIQUE USER IDENTIFIER
     * Used by client to reference user in subsequent requests
     * Example: 1
     */
    private Long id;
    
    /**
     * EMAIL - USER'S LOGIN EMAIL
     * Displayed to confirm logged-in user
     * Example: "rajesh@example.com"
     */
    private String email;
    
    /**
     * USERNAME - USER'S UNIQUE DISPLAY NAME
     * Shown in UI header, session timer, and communications
     * Used for display and personalization
     * Example: "rajesh_9876"
     */
    private String username;
    
    /**
     * ROLES - USER'S PERMISSION ROLES
     * Set of roles assigned to user (e.g., {"USER"}, {"ADMIN"})
     * Used by client to conditionally display UI features
     * Example: If user has "ADMIN" role, show admin panel
     */
    private Set<String> roles;
    
    /**
     * JWT TOKEN - BEARER TOKEN FOR AUTHENTICATION
     * Contains encoded user info and signature
     * Client must include in Authorization header: "Authorization: Bearer <token>"
     * Token expires after configured time (default: 24 hours)
     * Example: "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX1VTRVIiXSwic3ViIjoicmFqZXNoQGV4YW1wbGUuY29tIiwiaWF0IjoxNjYzOTU0Mzg3LCJleHAiOjE2NjQwMzgzODd9..."
     */
    private String token;

    private String refreshToken;

    private boolean requiresProfileCompletion;
    
    /**
     * TOKEN TYPE - HOW TO USE THE TOKEN
     * Always "Bearer" for JWT tokens
     * Indicates token should be used in format: "Bearer <token>"
     * Example: "Bearer"
     */
    private String tokenType = "Bearer";
    
    public AuthResponseDto() {}
    
    public AuthResponseDto(Long id, String email, String username, Set<String> roles, String token) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.roles = roles;
        this.token = token;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public boolean isRequiresProfileCompletion() { return requiresProfileCompletion; }
    public void setRequiresProfileCompletion(boolean requiresProfileCompletion) { this.requiresProfileCompletion = requiresProfileCompletion; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
}

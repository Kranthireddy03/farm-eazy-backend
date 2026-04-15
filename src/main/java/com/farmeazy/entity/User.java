package com.farmeazy.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

/**
 * USER ENTITY CLASS
 * 
 * PURPOSE: This class represents a user account in the FarmEazy system.
 * It stores all user information including personal details, authentication credentials, and farm management data.
 * 
 * KEY COMPONENTS:
 * 1. Personal Information: Full name, email, phone, address, city, state, pinCode
 * 2. Authentication: Email (unique identifier), encrypted password
 * 3. Status: Active flag to enable/disable user accounts
 * 4. Roles: Set of user roles (e.g., USER) for role-based access control
 * 5. Relationships: Links to multiple farms owned by this user
 * 6. Timestamps: createdAt (immutable) and updatedAt (auto-updated)
 * 
 * HOW IT WORKS:
 * - Each user can own multiple farms (One-to-Many relationship)
 * - User email is unique and used for login authentication
 * - Password is encrypted using BCrypt before storing in database
 * - Roles determine what actions the user can perform in the system
 * - Timestamps automatically track when user account was created and last modified
 * 
 * DATABASE TABLE: "users"
 * - Stores user account data for authentication and farm ownership
 */
@Entity
@Table(name = "users")
public class User {
    
    /**
     * UNIQUE IDENTIFIER FOR USER
        * - Auto-generated primary key using database identity
     * - Used to uniquely identify each user in the system
     */
    @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * USER EMAIL - UNIQUE LOGIN CREDENTIAL
     * - Cannot be null and must be unique across all users
     * - Used as the primary identifier for user authentication (login)
     * - Acts as username for JWT token generation
     */
    @Column(nullable = false, unique = true)
    private String email;
    
    /**
     * USERNAME - UNIQUE DISPLAY NAME
     * - Cannot be null and must be unique across all users
     * - Used for display in UI and communications
     * - Can be auto-generated from email/phone or user-provided
     * - Format: alphanumeric with underscores (e.g., "john_9876")
     */
    @Column(nullable = false, unique = true)
    private String username;
    
    /**
     * ENCRYPTED PASSWORD - LOGIN CREDENTIAL
     * - Cannot be null
     * - Encrypted using BCryptPasswordEncoder before storage
     * - Never transmitted or displayed in response DTOs
     * - Used with email/username for user authentication during login
     */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String authProvider = "PASSWORD";

    @Column(nullable = false)
    private Boolean profileCompleted = true;
    
    /**
     * PHONE NUMBER - USER'S CONTACT INFORMATION
     * - Optional field
     * - Expected format: 10 digits (India-specific: XXXXXXXXXX)
     * - Used for notifications and farm-related communications
     */
    @Column
    private String phone;
    
    /**
     * STREET ADDRESS - USER'S RESIDENTIAL ADDRESS
     * - Optional field
     * - Part of user profile information
     */
    @Column
    private String address;
    
    /**
     * CITY - USER'S CITY/TOWN
     * - Optional field
     * - Used to identify user's geographic location
     */
    @Column
    private String city;
    
    /**
     * STATE - USER'S STATE/PROVINCE
     * - Optional field
     * - Part of user's complete address
     */
    @Column
    private String state;
    
    /**
     * POSTAL CODE - USER'S PIN/ZIP CODE
     * - Optional field
     * - Used for geographic identification and postal services
     */
    @Column
    private String pinCode;
    
    /**
     * ACCOUNT ACTIVE STATUS
     * - Cannot be null, defaults to true
     * - Tracks whether user account is active or disabled
     * - When false, user cannot login even with correct credentials
     * - Used for soft-delete functionality (deactivating user accounts)
     */
    @Column(nullable = false)
    private Boolean active = true;
    
    /**
     * USER ROLES - DEFINES USER PERMISSIONS
     * - Collection of role strings (e.g., "USER", "ADMIN")
     * - Stored in separate "user_roles" table (ElementCollection)
     * - Eagerly fetched (EAGER) because roles needed on every authentication
     * - Used by Spring Security for @PreAuthorize checks
     * - Example: A user with "USER" role gets "ROLE_USER" authority
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();
    
    /**
     * ACCOUNT CREATION TIMESTAMP
     * - Cannot be null, not updatable (immutable)
     * - Automatically set when user account is first created
     * - Stored in database as LocalDateTime
     * - Used to track account age and audit trail
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * LAST MODIFICATION TIMESTAMP
     * - Cannot be null
     * - Automatically updated whenever user account is modified
     * - Used to track when user profile was last changed
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * RELATIONSHIP: ONE USER TO MANY FARMS
     * - mappedBy = "user": Farm entity has 'user' field that owns this relationship
     * - cascade = CascadeType.ALL: When user is deleted, all their farms are also deleted
     * - fetch = FetchType.LAZY: Farms are loaded only when explicitly accessed (performance optimization)
     * - Used to fetch all farms belonging to this user
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Farm> farms = new HashSet<>();
    
    public User() {}
    
    public User(Long id, String email, String password, String phone, String address, String city, String state, String pinCode, Boolean active, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pinCode = pinCode;
        this.active = active;
        this.roles = roles;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }
    public Boolean getProfileCompleted() { return profileCompleted; }
    public void setProfileCompleted(Boolean profileCompleted) { this.profileCompleted = profileCompleted; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Set<Farm> getFarms() { return farms; }
    public void setFarms(Set<Farm> farms) { this.farms = farms; }
    
    @PrePersist
    protected void onCreate() {
        if (authProvider == null || authProvider.isBlank()) {
            authProvider = "PASSWORD";
        }
        if (profileCompleted == null) {
            profileCompleted = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

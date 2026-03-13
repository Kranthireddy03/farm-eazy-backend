package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PUSH SUBSCRIPTION ENTITY
 * 
 * Stores browser push notification subscriptions for users.
 * Each user can have multiple subscriptions (different browsers/devices).
 * 
 * Web Push API requires:
 * - endpoint: Browser push service URL
 * - p256dh: Public key for encryption
 * - auth: Authentication secret
 */
@Entity
@Table(name = "push_subscriptions", indexes = {
    @Index(name = "idx_push_user", columnList = "user_id")
})
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Browser push service endpoint URL
     */
    @Column(name = "endpoint", length = 500, nullable = false, unique = true)
    private String endpoint;

    /**
     * P256dh public key for encryption
     */
    @Column(name = "p256dh", length = 200, nullable = false)
    private String p256dh;

    /**
     * Auth secret for message encryption
     */
    @Column(name = "auth", length = 100, nullable = false)
    private String auth;

    /**
     * User agent string (for debugging)
     */
    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUsedAt = LocalDateTime.now();
    }

    // Constructors
    public PushSubscription() {}

    public PushSubscription(User user, String endpoint, String p256dh, String auth) {
        this.user = user;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getP256dh() { return p256dh; }
    public void setP256dh(String p256dh) { this.p256dh = p256dh; }

    public String getAuth() { return auth; }
    public void setAuth(String auth) { this.auth = auth; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}

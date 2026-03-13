package com.farmeazy.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * PUSH SUBSCRIPTION DTO
 * 
 * Matches the browser's PushSubscription object structure.
 * 
 * Browser sends:
 * {
 *   "endpoint": "https://fcm.googleapis.com/fcm/send/...",
 *   "keys": {
 *     "p256dh": "base64-encoded-public-key",
 *     "auth": "base64-encoded-auth-secret"
 *   }
 * }
 */
public class PushSubscriptionDto {

    @NotBlank(message = "Endpoint is required")
    private String endpoint;

    private Keys keys;

    private String userAgent;

    // Default constructor
    public PushSubscriptionDto() {}

    // Getters and Setters
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public Keys getKeys() { return keys; }
    public void setKeys(Keys keys) { this.keys = keys; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    /**
     * Push subscription keys
     */
    public static class Keys {
        @NotBlank(message = "p256dh key is required")
        private String p256dh;

        @NotBlank(message = "auth key is required")
        private String auth;

        public Keys() {}

        public String getP256dh() { return p256dh; }
        public void setP256dh(String p256dh) { this.p256dh = p256dh; }

        public String getAuth() { return auth; }
        public void setAuth(String auth) { this.auth = auth; }
    }

    /**
     * Unsubscribe request
     */
    public static class UnsubscribeRequest {
        @NotBlank(message = "Endpoint is required")
        private String endpoint;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }

    /**
     * VAPID public key response
     */
    public static class VapidKeyResponse {
        private String publicKey;
        private boolean enabled;

        public VapidKeyResponse(String publicKey, boolean enabled) {
            this.publicKey = publicKey;
            this.enabled = enabled;
        }

        public String getPublicKey() { return publicKey; }
        public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}

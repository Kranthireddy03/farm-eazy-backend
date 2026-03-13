package com.farmeazy.service;

import com.farmeazy.dto.PushSubscriptionDto;
import com.farmeazy.entity.PushSubscription;
import com.farmeazy.entity.User;
import com.farmeazy.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PUSH NOTIFICATION SERVICE
 * 
 * Handles browser push notifications using Web Push API.
 * 
 * How it works:
 * 1. Frontend requests permission and gets a PushSubscription from browser
 * 2. Frontend sends subscription to backend via /api/push/subscribe
 * 3. When notification occurs, backend sends push to all user's subscriptions
 * 4. Browser's Service Worker receives and displays the notification
 * 
 * VAPID (Voluntary Application Server Identification):
 * - Public key: Given to frontend for subscription
 * - Private key: Used by backend to sign push messages
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    @Autowired
    private PushSubscriptionRepository subscriptionRepository;

    @Value("${push.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${push.vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${push.vapid.subject:mailto:support@farmeazy.com}")
    private String vapidSubject;

    @Value("${push.enabled:false}")
    private boolean pushEnabled;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Get VAPID public key for frontend
     */
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    /**
     * Check if push notifications are enabled
     */
    public boolean isPushEnabled() {
        return pushEnabled && vapidPublicKey != null && !vapidPublicKey.isEmpty();
    }

    /**
     * Subscribe user to push notifications
     */
    @Transactional
    public PushSubscription subscribe(User user, PushSubscriptionDto dto) {
        // Check if already subscribed with this endpoint
        if (subscriptionRepository.existsByEndpoint(dto.getEndpoint())) {
            log.info("PUSH_ALREADY_SUBSCRIBED: userId={}, endpoint=..{}", 
                    user.getId(), dto.getEndpoint().substring(dto.getEndpoint().length() - 20));
            return subscriptionRepository.findByEndpoint(dto.getEndpoint()).orElse(null);
        }

        PushSubscription subscription = new PushSubscription(
                user,
                dto.getEndpoint(),
                dto.getKeys().getP256dh(),
                dto.getKeys().getAuth()
        );
        subscription.setUserAgent(dto.getUserAgent());

        PushSubscription saved = subscriptionRepository.save(subscription);
        log.info("PUSH_SUBSCRIBED: userId={}, subscriptionId={}", user.getId(), saved.getId());
        return saved;
    }

    /**
     * Unsubscribe from push notifications
     */
    @Transactional
    public void unsubscribe(String endpoint) {
        subscriptionRepository.deleteByEndpoint(endpoint);
        log.info("PUSH_UNSUBSCRIBED: endpoint=..{}", endpoint.substring(endpoint.length() - 20));
    }

    /**
     * Send push notification to a user (all their devices)
     */
    @Async
    public void sendToUser(User user, String title, String message, String url) {
        if (!isPushEnabled()) {
            log.debug("PUSH_DISABLED: Would send to userId={}", user.getId());
            return;
        }

        List<PushSubscription> subscriptions = subscriptionRepository.findByUser(user);
        for (PushSubscription sub : subscriptions) {
            sendPush(sub, title, message, url);
        }
    }

    /**
     * Broadcast push to all subscribed users
     */
    @Async
    public void broadcast(String title, String message, String url) {
        if (!isPushEnabled()) {
            log.debug("PUSH_DISABLED: Would broadcast");
            return;
        }

        List<PushSubscription> allSubscriptions = subscriptionRepository.findAllWithUser();
        log.info("PUSH_BROADCAST: Sending to {} subscriptions", allSubscriptions.size());
        
        for (PushSubscription sub : allSubscriptions) {
            sendPush(sub, title, message, url);
        }
    }

    /**
     * Send push notification to a specific subscription
     */
    private void sendPush(PushSubscription subscription, String title, String message, String url) {
        try {
            // Build push payload
            String payload = buildPayload(title, message, url);
            
            // For now, log the push (actual Web Push requires crypto library)
            // In production, use webpush-java or similar library
            log.info("PUSH_SEND: subscriptionId={}, title={}", subscription.getId(), title);
            
            // Update last used time
            subscription.setLastUsedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            // TODO: Implement actual Web Push sending
            // This requires webpush-java library and VAPID key signing
            // For now, this logs the push attempt
            
        } catch (Exception e) {
            log.error("PUSH_FAILED: subscriptionId={}, error={}", subscription.getId(), e.getMessage());
            // If push fails with 404/410, subscription is invalid - delete it
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("410"))) {
                subscriptionRepository.delete(subscription);
                log.info("PUSH_SUBSCRIPTION_REMOVED: Invalid subscription deleted");
            }
        }
    }

    /**
     * Build push notification payload JSON
     */
    private String buildPayload(String title, String message, String url) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"title\":\"").append(escapeJson(title)).append("\",");
        json.append("\"body\":\"").append(escapeJson(message)).append("\",");
        json.append("\"icon\":\"/logo192.png\",");
        json.append("\"badge\":\"/badge.png\",");
        if (url != null && !url.isEmpty()) {
            json.append("\"url\":\"").append(escapeJson(url)).append("\",");
        }
        json.append("\"timestamp\":").append(System.currentTimeMillis());
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

package com.farmeazy.controller;

import com.farmeazy.dto.PushSubscriptionDto;
import com.farmeazy.entity.PushSubscription;
import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * PUSH NOTIFICATION CONTROLLER
 * 
 * Endpoints for browser push notification subscription management.
 * 
 * Endpoints:
 * - GET /api/push/vapid-key - Get VAPID public key for frontend
 * - POST /api/push/subscribe - Register push subscription
 * - POST /api/push/unsubscribe - Remove push subscription
 */
@RestController
@RequestMapping("/api/push")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
public class PushNotificationController {

    @Autowired
    private PushNotificationService pushService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * GET /api/push/vapid-key
     * Get VAPID public key for frontend to create subscription
     * This endpoint should be public (no auth required)
     */
    @GetMapping("/vapid-key")
    public ResponseEntity<PushSubscriptionDto.VapidKeyResponse> getVapidKey() {
        return ResponseEntity.ok(new PushSubscriptionDto.VapidKeyResponse(
                pushService.getVapidPublicKey(),
                pushService.isPushEnabled()
        ));
    }

    /**
     * POST /api/push/subscribe
     * Register a push subscription for the current user
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(
            @Valid @RequestBody PushSubscriptionDto subscription) {
        
        User user = getCurrentUser();
        PushSubscription saved = pushService.subscribe(user, subscription);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Push subscription registered");
        response.put("subscriptionId", saved != null ? saved.getId() : null);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/push/unsubscribe
     * Remove a push subscription
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(
            @Valid @RequestBody PushSubscriptionDto.UnsubscribeRequest request) {
        
        pushService.unsubscribe(request.getEndpoint());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Push subscription removed");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/push/status
     * Check push notification status for current user
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        User user = getCurrentUser();

        Map<String, Object> response = new HashMap<>();
        response.put("pushEnabled", pushService.isPushEnabled());
        response.put("vapidKeyAvailable", pushService.getVapidPublicKey() != null && !pushService.getVapidPublicKey().isEmpty());
        return ResponseEntity.ok(response);
    }
}

package com.farmeazy.controller;

import com.farmeazy.dto.NotificationDto;
import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NOTIFICATION CONTROLLER
 * 
 * User-facing endpoints for notification management.
 * 
 * Endpoints:
 * - GET /api/notifications - Get all notifications
 * - GET /api/notifications/count - Get unread count for badge
 * - GET /api/notifications/recent - Get recent notifications (limited)
 * - PUT /api/notifications/{id}/read - Mark as read
 * - PUT /api/notifications/read-all - Mark all as read
 * - DELETE /api/notifications/{id} - Dismiss notification
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("User not authenticated");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("User not found"));
    }

    /**
     * GET /api/notifications
     * Get all active notifications for current user
     */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications() {
        User user = getCurrentUser();
        List<NotificationDto> notifications = notificationService.getNotificationsForUser(user);
        return ResponseEntity.ok(notifications);
    }

    /**
     * GET /api/notifications/count
     * Get unread notification count for bell badge
     */
    @GetMapping("/count")
    public ResponseEntity<NotificationDto.NotificationCountResponse> getUnreadCount() {
        User user = getCurrentUser();
        NotificationDto.NotificationCountResponse count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(count);
    }

    /**
     * GET /api/notifications/recent?limit=10
     * Get recent notifications (for dropdown)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<NotificationDto>> getRecentNotifications(
            @RequestParam(defaultValue = "10") int limit) {
        User user = getCurrentUser();
        List<NotificationDto> notifications = notificationService.getRecentNotifications(user, limit);
        return ResponseEntity.ok(notifications);
    }

    /**
     * PUT /api/notifications/{id}/read
     * Mark a notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        User user = getCurrentUser();
        notificationService.markAsRead(id, user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification marked as read");
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/notifications/read-all
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        User user = getCurrentUser();
        int count = notificationService.markAllAsRead(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/notifications/{id}
     * Dismiss/delete a notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> dismissNotification(@PathVariable Long id) {
        User user = getCurrentUser();
        notificationService.dismiss(id, user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification dismissed");
        return ResponseEntity.ok(response);
    }
}

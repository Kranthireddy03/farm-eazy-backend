package com.farmeazy.controller;

import com.farmeazy.dto.NotificationDto;
import com.farmeazy.entity.Notification;
import com.farmeazy.entity.Notification.NotificationPriority;
import com.farmeazy.entity.Notification.NotificationType;
import com.farmeazy.entity.User;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.NotificationRepository;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ADMIN NOTIFICATION CONTROLLER
 * 
 * Admin-only endpoints for managing notifications system-wide.
 * 
 * Endpoints:
 * - POST /api/admin/notifications/broadcast - Send notification to all users
 * - POST /api/admin/notifications/user/{userId} - Send to specific user
 * - GET /api/admin/notifications/broadcasts - Get all broadcast notifications
 * - DELETE /api/admin/notifications/{id} - Delete notification globally
 * - GET /api/admin/users - List all users (for targeting)
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Verify admin role
     */
    private User verifyAdminAndGetUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getRoles().contains("ADMIN")) {
            throw new UnauthorizedException("Admin access required");
        }
        return user;
    }

    /**
     * POST /api/admin/notifications/broadcast
     * Send a broadcast notification to all users
     */
    @PostMapping("/notifications/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastNotification(
            @Valid @RequestBody NotificationDto.CreateNotificationRequest request) {
        
        verifyAdminAndGetUser();

        NotificationType type = NotificationType.valueOf(request.getType().toUpperCase());
        NotificationPriority priority = NotificationPriority.valueOf(request.getPriority().toUpperCase());

        Notification notification = notificationService.broadcast(
                type,
                request.getTitle(),
                request.getMessage(),
                request.getActionUrl(),
                priority,
                request.getExpiresInDays()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Broadcast notification sent successfully");
        response.put("notificationId", notification.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/admin/notifications/user/{userId}
     * Send notification to a specific user
     */
    @PostMapping("/notifications/user/{userId}")
    public ResponseEntity<Map<String, Object>> sendToUser(
            @PathVariable Long userId,
            @Valid @RequestBody NotificationDto.CreateNotificationRequest request) {
        
        verifyAdminAndGetUser();

        NotificationType type = NotificationType.valueOf(request.getType().toUpperCase());
        NotificationPriority priority = NotificationPriority.valueOf(request.getPriority().toUpperCase());

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Notification notification = notificationService.createForUser(
                targetUser,
                type,
                request.getTitle(),
                request.getMessage(),
                request.getActionUrl(),
                priority
        );

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Notification sent to user " + userId);
        response.put("notificationId", notification.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/notifications/broadcasts
     * Get all broadcast notifications (for admin dashboard)
     */
    @GetMapping("/notifications/broadcasts")
    public ResponseEntity<List<NotificationDto>> getBroadcasts() {
        verifyAdminAndGetUser();

        List<Notification> broadcasts = notificationRepository.findByIsBroadcastTrueOrderByCreatedAtDesc();
        List<NotificationDto> dtos = broadcasts.stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * DELETE /api/admin/notifications/{id}
     * Delete a notification globally (admin only)
     */
    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        verifyAdminAndGetUser();

        notificationRepository.deleteById(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification deleted");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/users
     * Get list of users (for targeting notifications)
     */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        verifyAdminAndGetUser();

        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = users.stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("email", user.getEmail());
                    map.put("username", user.getUsername());
                    map.put("phone", user.getPhone());
                    map.put("active", user.getActive());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(userList);
    }

    /**
     * GET /api/admin/notification-types
     * Get available notification types (for dropdown)
     */
    @GetMapping("/notification-types")
    public ResponseEntity<List<String>> getNotificationTypes() {
        verifyAdminAndGetUser();

        List<String> types = List.of(
                "ORDER", "PAYMENT", "FARM", "IRRIGATION", 
                "PRODUCT", "ACCOUNT", "SYSTEM", "PROMO"
        );
        return ResponseEntity.ok(types);
    }

    /**
     * GET /api/admin/dashboard/stats
     * Get admin dashboard statistics
     */
    @GetMapping("/dashboard/notification-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        verifyAdminAndGetUser();

        long totalUsers = userRepository.count();
        long totalBroadcasts = notificationRepository.findByIsBroadcastTrueOrderByCreatedAtDesc().size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalBroadcasts", totalBroadcasts);
        return ResponseEntity.ok(stats);
    }
}

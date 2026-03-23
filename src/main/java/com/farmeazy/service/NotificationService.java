package com.farmeazy.service;

import com.farmeazy.dto.NotificationDto;
import com.farmeazy.entity.Notification;
import com.farmeazy.entity.Notification.NotificationPriority;
import com.farmeazy.entity.Notification.NotificationType;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.NotificationRepository;
import com.farmeazy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NOTIFICATION SERVICE
 * 
 * Handles all notification operations:
 * - Creating user-specific notifications
 * - Broadcasting to all users
 * - Retrieving and dismissing notifications
 * - Cleanup of old notifications
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // ==================== CREATE NOTIFICATIONS ====================

    /**
     * Create a notification for a specific user
     */
    @Transactional
    public Notification createForUser(User user, NotificationType type, String title, String message) {
        return createForUser(user, type, title, message, null, NotificationPriority.NORMAL);
    }

    /**
     * Create a notification for a specific user with action URL
     */
    @Transactional
    public Notification createForUser(User user, NotificationType type, String title, String message, 
                                      String actionUrl, NotificationPriority priority) {
        Notification notification = new Notification(user, type, title, message);
        notification.setActionUrl(actionUrl);
        notification.setPriority(priority);
        
        Notification saved = notificationRepository.save(notification);
        log.info("NOTIF_CREATED: userId={}, type={}, title={}", user.getId(), type, title);
        return saved;
    }

    /**
     * Create notification by user ID
     */
    @Transactional
    public Notification createForUserId(Long userId, NotificationType type, String title, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return createForUser(user, type, title, message);
    }

    /**
     * Broadcast notification to all users
     */
    @Transactional
    public Notification broadcast(NotificationType type, String title, String message, 
                                  String actionUrl, NotificationPriority priority, Integer expiresInDays) {
        Notification notification = Notification.broadcast(type, title, message);
        notification.setActionUrl(actionUrl);
        notification.setPriority(priority);
        if (expiresInDays != null) {
            notification.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        }
        
        Notification saved = notificationRepository.save(notification);
        log.info("NOTIF_BROADCAST: type={}, title={}, expiresIn={}days", type, title, expiresInDays);
        return saved;
    }

    // ==================== RETRIEVE NOTIFICATIONS ====================

    /**
     * Get all active notifications for a user (includes broadcasts)
     */
    public List<NotificationDto> getNotificationsForUser(User user) {
        List<Notification> notifications = notificationRepository.findAllActiveForUser(
                user, 
                String.valueOf(user.getId()), 
                LocalDateTime.now()
        );
        return notifications.stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get recent notifications (limited for performance)
     */
    public List<NotificationDto> getRecentNotifications(User user, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<Notification> notifications = notificationRepository.findRecentForUser(
                user, 
                String.valueOf(user.getId()), 
                LocalDateTime.now(),
            PageRequest.of(0, safeLimit)
        );
        return notifications.stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get unread count for badge
     */
    public NotificationDto.NotificationCountResponse getUnreadCount(User user) {
        Long unread = notificationRepository.countUnreadForUser(
                user, 
                String.valueOf(user.getId()), 
                LocalDateTime.now()
        );
        return new NotificationDto.NotificationCountResponse(unread, unread);
    }

    // ==================== UPDATE NOTIFICATIONS ====================

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // For broadcast notifications, add to dismissed list
        if (notification.getIsBroadcast()) {
            notification.dismissForUser(user.getId());
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        } else if (notification.getUser().getId().equals(user.getId())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        
        notificationRepository.save(notification);
        log.debug("NOTIF_READ: id={}, userId={}", notificationId, user.getId());
    }

    /**
     * Mark all as read for user
     */
    @Transactional
    public int markAllAsRead(User user) {
        int count = notificationRepository.markAllAsReadForUser(user, LocalDateTime.now());
        log.info("NOTIF_READ_ALL: userId={}, count={}", user.getId(), count);
        return count;
    }

    /**
     * Dismiss notification (user won't see it again)
     */
    @Transactional
    public void dismiss(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (notification.getIsBroadcast()) {
            notification.dismissForUser(user.getId());
            notificationRepository.save(notification);
        } else if (notification.getUser().getId().equals(user.getId())) {
            notificationRepository.delete(notification);
        }
        
        log.debug("NOTIF_DISMISSED: id={}, userId={}", notificationId, user.getId());
    }

    // ==================== CLEANUP JOBS ====================

    /**
     * Cleanup expired notifications - runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredNotifications() {
        int deletedExpired = notificationRepository.deleteExpiredNotifications(LocalDateTime.now());
        int deletedOldRead = notificationRepository.deleteOldReadNotifications(
                LocalDateTime.now().minusDays(7) // Delete read notifications older than 7 days
        );
        log.info("NOTIF_CLEANUP: expired={}, oldRead={}", deletedExpired, deletedOldRead);
    }

    // ==================== CONVENIENCE METHODS FOR SERVICES ====================

    /**
     * Send welcome notification on registration
     */
    public void sendWelcomeNotification(User user) {
        createForUser(user, NotificationType.ACCOUNT, 
                "Welcome to FarmEazy!", 
                "Your account has been created successfully. Start managing your farms today!",
                "/dashboard",
                NotificationPriority.NORMAL);
    }

    /**
     * Send order notification
     */
    public void sendOrderNotification(User user, String orderId, String status) {
        String title = "Order " + status;
        String message = "Your order #" + orderId + " has been " + status.toLowerCase() + ".";
        createForUser(user, NotificationType.ORDER, title, message, "/orders/" + orderId, NotificationPriority.NORMAL);
    }

    /**
     * Send payment notification
     */
    public void sendPaymentNotification(User user, boolean success, String amount, String orderId) {
        NotificationType type = NotificationType.PAYMENT;
        NotificationPriority priority = success ? NotificationPriority.NORMAL : NotificationPriority.HIGH;
        String title = success ? "Payment Successful" : "Payment Failed";
        String message = success 
                ? "Payment of ₹" + amount + " for order #" + orderId + " was successful."
                : "Payment of ₹" + amount + " for order #" + orderId + " failed. Please try again.";
        createForUser(user, type, title, message, "/orders/" + orderId, priority);
    }

    /**
     * Send irrigation reminder
     */
    public void sendIrrigationReminder(User user, String cropName, String farmName) {
        createForUser(user, NotificationType.IRRIGATION,
                "Irrigation Reminder",
                "Your " + cropName + " in " + farmName + " needs irrigation today.",
                "/farms",
                NotificationPriority.HIGH);
    }
}

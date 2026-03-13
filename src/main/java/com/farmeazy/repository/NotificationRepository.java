package com.farmeazy.repository;

import com.farmeazy.entity.Notification;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NOTIFICATION REPOSITORY
 * 
 * Efficient queries for notification retrieval and cleanup.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Get user's direct notifications (not read, ordered by date)
     */
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    /**
     * Get all notifications for a user (direct + broadcasts not dismissed)
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.user = :user OR (n.isBroadcast = true AND (n.dismissedBy IS NULL OR n.dismissedBy NOT LIKE CONCAT('%', :userId, '%')))) " +
           "AND n.expiresAt > :now " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findAllActiveForUser(@Param("user") User user, 
                                            @Param("userId") String userId,
                                            @Param("now") LocalDateTime now);

    /**
     * Count unread notifications for user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE " +
           "((n.user = :user AND n.isRead = false) OR " +
           "(n.isBroadcast = true AND n.isRead = false AND (n.dismissedBy IS NULL OR n.dismissedBy NOT LIKE CONCAT('%', :userId, '%')))) " +
           "AND n.expiresAt > :now")
    Long countUnreadForUser(@Param("user") User user, 
                            @Param("userId") String userId,
                            @Param("now") LocalDateTime now);

    /**
     * Get all broadcast notifications (for admin)
     */
    List<Notification> findByIsBroadcastTrueOrderByCreatedAtDesc();

    /**
     * Delete expired notifications (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt < :now")
    int deleteExpiredNotifications(@Param("now") LocalDateTime now);

    /**
     * Delete old read notifications (keep DB clean)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.readAt < :cutoff")
    int deleteOldReadNotifications(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Get recent notifications (limit for performance)
     */
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.user = :user OR (n.isBroadcast = true AND (n.dismissedBy IS NULL OR n.dismissedBy NOT LIKE CONCAT('%', :userId, '%')))) " +
           "AND n.expiresAt > :now " +
           "ORDER BY n.createdAt DESC " +
           "LIMIT :limit")
    List<Notification> findRecentForUser(@Param("user") User user, 
                                         @Param("userId") String userId,
                                         @Param("now") LocalDateTime now,
                                         @Param("limit") int limit);

    /**
     * Mark all as read for user
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadForUser(@Param("user") User user, @Param("now") LocalDateTime now);
}

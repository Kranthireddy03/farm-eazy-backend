package com.farmeazy.repository;

import com.farmeazy.entity.IrrigationRemindersLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for IrrigationRemindersLog entity
 * Tracks all reminder notifications sent to farmers
 */
@Repository
public interface IrrigationRemindersLogRepository extends JpaRepository<IrrigationRemindersLog, Long> {

    /**
     * Count reminders with specific status
     */
    @Query("SELECT COUNT(rl) FROM IrrigationRemindersLog rl WHERE rl.status = :status")
    Long countByStatus(@Param("status") String status);

    /**
     * Find failed reminders needing retry
     */
    @Query("SELECT rl FROM IrrigationRemindersLog rl " +
           "WHERE rl.status = 'FAILED' " +
           "AND rl.retryCount < rl.maxRetries " +
           "ORDER BY rl.sentAt ASC")
    List<IrrigationRemindersLog> findFailedRemindersForRetry();

    /**
     * Find reminders sent in last N hours
     */
    @Query("SELECT rl FROM IrrigationRemindersLog rl " +
           "WHERE rl.sentAt > :since " +
           "ORDER BY rl.sentAt DESC")
    List<IrrigationRemindersLog> findRecentReminders(@Param("since") LocalDateTime since);

    /**
     * Find reminders by user and status
     */
    @Query("SELECT rl FROM IrrigationRemindersLog rl " +
           "WHERE rl.userId = :userId " +
           "AND rl.status = :status " +
           "ORDER BY rl.sentAt DESC")
    List<IrrigationRemindersLog> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") String status
    );

    /**
     * Find reminders by farm
     */
    @Query("SELECT rl FROM IrrigationRemindersLog rl " +
           "WHERE rl.farmId = :farmId " +
           "ORDER BY rl.sentAt DESC")
    List<IrrigationRemindersLog> findByFarmId(@Param("farmId") Long farmId);

    /**
     * Count reminders by type and status
     */
    @Query("SELECT COUNT(rl) FROM IrrigationRemindersLog rl " +
           "WHERE rl.reminderType = :type " +
           "AND rl.status = :status " +
           "AND rl.sentAt > :since")
    Long countByTypeAndStatus(
        @Param("type") String type,
        @Param("status") String status,
        @Param("since") LocalDateTime since
    );

    /**
     * Calculate delivery rate for monitoring
     */
    @Query("SELECT COUNT(CASE WHEN rl.status = 'DELIVERED' THEN 1 END) * 100.0 / COUNT(rl) " +
           "FROM IrrigationRemindersLog rl " +
           "WHERE rl.sentAt > :since")
    Double getDeliveryRate(@Param("since") LocalDateTime since);
}

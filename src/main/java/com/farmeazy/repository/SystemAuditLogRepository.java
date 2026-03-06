package com.farmeazy.repository;

import com.farmeazy.entity.SystemAuditLog;
import com.farmeazy.entity.SystemAuditLog.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SYSTEM AUDIT LOG REPOSITORY
 * 
 * PURPOSE: Data access layer for security audit logs.
 * Provides methods to query security events and suspicious activities.
 */
@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, Long> {

    /**
     * Find by user ID with pagination.
     */
    Page<SystemAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find by event type.
     */
    Page<SystemAuditLog> findByEventTypeOrderByCreatedAtDesc(EventType eventType, Pageable pageable);

    /**
     * Find suspicious activities.
     */
    List<SystemAuditLog> findByIsSuspiciousTrueOrderByCreatedAtDesc();

    /**
     * Find by IP address.
     */
    List<SystemAuditLog> findByIpAddressOrderByCreatedAtDesc(String ipAddress);

    /**
     * Find recent failed logins.
     */
    @Query("SELECT s FROM SystemAuditLog s WHERE s.eventType = 'LOGIN_FAILED' " +
           "AND s.createdAt >= :since ORDER BY s.createdAt DESC")
    List<SystemAuditLog> findRecentFailedLogins(@Param("since") LocalDateTime since);

    /**
     * Count failed logins by IP.
     */
    @Query("SELECT COUNT(s) FROM SystemAuditLog s WHERE s.eventType = 'LOGIN_FAILED' " +
           "AND s.ipAddress = :ipAddress AND s.createdAt >= :since")
    long countFailedLoginsByIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    /**
     * Count events by type.
     */
    @Query("SELECT s.eventType, COUNT(s) FROM SystemAuditLog s " +
           "WHERE s.createdAt >= :since GROUP BY s.eventType")
    List<Object[]> countByEventType(@Param("since") LocalDateTime since);

    /**
     * Find high risk events.
     */
    @Query("SELECT s FROM SystemAuditLog s WHERE s.riskScore >= :minRisk " +
           "ORDER BY s.riskScore DESC, s.createdAt DESC")
    List<SystemAuditLog> findHighRiskEvents(@Param("minRisk") int minRisk);

    /**
     * Find events by resource.
     */
    List<SystemAuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String resourceType, String resourceId);
}

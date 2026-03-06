package com.farmeazy.repository;

import com.farmeazy.entity.Order;
import com.farmeazy.entity.RefundAuditLog;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for RefundAuditLog entity.
 * Tracks all refund-related actions for compliance.
 */
@Repository
public interface RefundAuditLogRepository extends JpaRepository<RefundAuditLog, Long> {

    /**
     * Find all audit logs for an order.
     */
    List<RefundAuditLog> findByOrderOrderByCreatedAtDesc(Order order);

    /**
     * Find all audit logs for an order by order ID.
     */
    List<RefundAuditLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /**
     * Find all audit logs for a user.
     */
    List<RefundAuditLog> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find audit logs by action type.
     */
    List<RefundAuditLog> findByActionOrderByCreatedAtDesc(String action);

    /**
     * Find audit logs within a date range.
     */
    List<RefundAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count audit logs by action.
     */
    long countByAction(String action);
}

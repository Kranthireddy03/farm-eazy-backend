package com.farmeazy.repository;

import com.farmeazy.entity.Order;
import com.farmeazy.entity.Order.RefundStatus;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    // List<Order> findBySellerOrderByCreatedAtDesc(User seller);
    Optional<Order> findByIdAndUser(Long id, User user);
    Long countByUser(User user);

    // Refund-related queries
    /**
     * Find orders by refund status.
     */
    List<Order> findByRefundStatus(RefundStatus refundStatus);

    /**
     * Find orders with pending refunds (REQUESTED or APPROVED).
     */
    @Query("SELECT o FROM Order o WHERE o.refundStatus IN ('REQUESTED', 'APPROVED', 'PROCESSING')")
    List<Order> findPendingRefunds();

    /**
     * Find orders eligible for refund processing (APPROVED status).
     */
    List<Order> findByRefundStatusAndRefundAttemptsLessThan(RefundStatus status, Integer maxAttempts);

    /**
     * Find user's orders with refund requests.
     */
    List<Order> findByUserAndRefundStatusNotOrderByRefundRequestedAtDesc(User user, RefundStatus status);

    /**
     * Find orders with failed refunds for retry.
     */
    @Query("SELECT o FROM Order o WHERE o.refundStatus = 'FAILED' AND o.refundAttempts < :maxAttempts")
    List<Order> findFailedRefundsForRetry(@Param("maxAttempts") Integer maxAttempts);

    /**
     * Find orders by Razorpay payment ID.
     */
    Optional<Order> findByRazorpayPaymentId(String razorpayPaymentId);

    /**
     * Count refunds by status.
     */
    long countByRefundStatus(RefundStatus refundStatus);

    /**
     * Find orders refunded within a date range.
     */
    List<Order> findByRefundCompletedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}

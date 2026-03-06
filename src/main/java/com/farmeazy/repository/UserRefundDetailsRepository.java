package com.farmeazy.repository;

import com.farmeazy.entity.User;
import com.farmeazy.entity.UserRefundDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserRefundDetails entity.
 * Manages buyer's refund bank/UPI details.
 */
@Repository
public interface UserRefundDetailsRepository extends JpaRepository<UserRefundDetails, Long> {

    /**
     * Find refund details by user.
     */
    Optional<UserRefundDetails> findByUser(User user);

    /**
     * Find refund details by user ID.
     */
    Optional<UserRefundDetails> findByUserId(Long userId);

    /**
     * Check if user has refund details.
     */
    boolean existsByUser(User user);

    /**
     * Check if user has refund details by user ID.
     */
    boolean existsByUserId(Long userId);

    /**
     * Delete refund details by user.
     */
    void deleteByUser(User user);
}

package com.farmeazy.repository;

import com.farmeazy.entity.PushSubscription;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PUSH SUBSCRIPTION REPOSITORY
 */
@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    /**
     * Find all subscriptions for a user
     */
    List<PushSubscription> findByUser(User user);

    /**
     * Find subscription by endpoint (unique)
     */
    Optional<PushSubscription> findByEndpoint(String endpoint);

    /**
     * Check if endpoint already exists
     */
    boolean existsByEndpoint(String endpoint);

    /**
     * Get all active subscriptions (for broadcast)
     */
    @Query("SELECT ps FROM PushSubscription ps JOIN FETCH ps.user")
    List<PushSubscription> findAllWithUser();

    /**
     * Delete subscriptions for a user
     */
    @Modifying
    void deleteByUser(User user);

    /**
     * Delete by endpoint
     */
    @Modifying
    void deleteByEndpoint(String endpoint);
}

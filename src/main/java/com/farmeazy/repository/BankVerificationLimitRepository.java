package com.farmeazy.repository;

import com.farmeazy.entity.BankVerificationLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * BANK VERIFICATION LIMIT REPOSITORY
 * 
 * PURPOSE: Data access layer for user verification limits.
 * Provides methods to check and update verification quotas.
 */
@Repository
public interface BankVerificationLimitRepository extends JpaRepository<BankVerificationLimit, Long> {

    /**
     * Find limit by user ID.
     */
    Optional<BankVerificationLimit> findByUserId(Long userId);

    /**
     * Find blocked users.
     */
    List<BankVerificationLimit> findByIsBlockedTrueOrderByBlockedAtDesc();

    /**
     * Find users needing daily reset.
     */
    @Query("SELECT b FROM BankVerificationLimit b WHERE b.lastVerificationDate < :today " +
           "AND b.usedToday > 0")
    List<BankVerificationLimit> findUsersNeedingDailyReset(@Param("today") LocalDate today);

    /**
     * Reset daily counters batch operation.
     */
    @Modifying
    @Query("UPDATE BankVerificationLimit b SET b.usedToday = 0 WHERE b.lastVerificationDate < :today")
    int resetDailyCounters(@Param("today") LocalDate today);

    /**
     * Reset monthly counters batch operation.
     */
    @Modifying
    @Query("UPDATE BankVerificationLimit b SET b.usedThisMonth = 0, b.lastResetDate = :today")
    int resetMonthlyCounters(@Param("today") LocalDate today);

    /**
     * Find users approaching total limit.
     */
    @Query("SELECT b FROM BankVerificationLimit b WHERE b.totalUsed >= (b.totalLimit * 0.8)")
    List<BankVerificationLimit> findUsersApproachingTotalLimit();
}

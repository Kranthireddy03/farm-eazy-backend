package com.farmeazy.repository;

import com.farmeazy.entity.BankVerificationRequest;
import com.farmeazy.entity.BankVerificationRequest.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * BANK VERIFICATION REQUEST REPOSITORY
 * 
 * PURPOSE: Data access layer for bank verification tracking.
 * Provides methods to query and manage verification requests.
 */
@Repository
public interface BankVerificationRequestRepository extends JpaRepository<BankVerificationRequest, Long> {

    /**
     * Find by verification number.
     */
    Optional<BankVerificationRequest> findByVerificationNumber(String verificationNumber);

    /**
     * Find requests by user ID.
     */
    Page<BankVerificationRequest> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find requests by status.
     */
    List<BankVerificationRequest> findByStatusOrderByCreatedAtAsc(VerificationStatus status);

    /**
     * Find pending transfers for batch processing.
     */
    @Query("SELECT b FROM BankVerificationRequest b WHERE b.status = 'TRANSFER_PENDING' " +
           "AND b.dailyLimitReached = false ORDER BY b.createdAt ASC")
    List<BankVerificationRequest> findPendingTransfers();

    /**
     * Count today's verification attempts by user.
     */
    @Query("SELECT COUNT(b) FROM BankVerificationRequest b WHERE b.user.id = :userId " +
           "AND b.lastVerificationDate = :today")
    int countTodayAttemptsByUser(@Param("userId") Long userId, @Param("today") LocalDate today);

    /**
     * Find expired verifications for cleanup.
     */
    @Query("SELECT b FROM BankVerificationRequest b WHERE b.status NOT IN ('VERIFIED', 'REJECTED', 'CANCELLED') " +
           "AND b.expiresAt < :now")
    List<BankVerificationRequest> findExpiredVerifications(@Param("now") LocalDateTime now);

    /**
     * Find verification by account number hash.
     */
    List<BankVerificationRequest> findByUserIdAndAccountNumberHashAndStatus(
            Long userId, String accountNumberHash, VerificationStatus status);

    /**
     * Find latest verification for user.
     */
    Optional<BankVerificationRequest> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}

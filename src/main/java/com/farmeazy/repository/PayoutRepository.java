package com.farmeazy.repository;

import com.farmeazy.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    List<Payout> findByUser_Id(Long userId);

    List<Payout> findByStatus(Payout.PayoutStatus status);

    List<Payout> findByReferenceTypeAndReferenceId(Payout.ReferenceType referenceType, Long referenceId);

    @Query("SELECT p FROM Payout p WHERE p.status = :status ORDER BY p.createdAt ASC")
    List<Payout> findPendingPayouts(@Param("status") Payout.PayoutStatus status);

    @Query("SELECT SUM(p.netAmount) FROM Payout p WHERE p.user.id = :userId AND p.status = 'COMPLETED'")
    java.math.BigDecimal getTotalPayoutsByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(p.platformFee) FROM Payout p WHERE p.status = 'COMPLETED'")
    java.math.BigDecimal getTotalPlatformEarnings();

    boolean existsByReferenceTypeAndReferenceId(Payout.ReferenceType referenceType, Long referenceId);
}

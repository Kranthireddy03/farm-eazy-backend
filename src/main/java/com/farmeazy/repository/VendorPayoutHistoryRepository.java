package com.farmeazy.repository;

import com.farmeazy.entity.VendorPayoutHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VendorPayoutHistoryRepository extends JpaRepository<VendorPayoutHistory, Long> {
    
    Page<VendorPayoutHistory> findByVendorIdOrderByCreatedAtDesc(Long vendorId, Pageable pageable);
    
    List<VendorPayoutHistory> findByVendorIdAndPayoutStatusOrderByCreatedAtDesc(Long vendorId, String payoutStatus);
    
    @Query("SELECT SUM(v.amount) FROM VendorPayoutHistory v WHERE v.vendorId = :vendorId AND v.payoutStatus = 'COMPLETED'")
    java.math.BigDecimal getTotalPayoutsForVendor(@Param("vendorId") Long vendorId);
    
    @Query("SELECT COALESCE(COUNT(v), 0) FROM VendorPayoutHistory v WHERE v.vendorId = :vendorId AND v.payoutStatus = 'PENDING'")
    Long getPendingPayoutCountForVendor(@Param("vendorId") Long vendorId);
    
    @Query("SELECT COALESCE(SUM(v.amount), 0) FROM VendorPayoutHistory v WHERE v.vendorId = :vendorId AND v.payoutStatus = 'PENDING'")
    java.math.BigDecimal getPendingAmountForVendor(@Param("vendorId") Long vendorId);
    
    List<VendorPayoutHistory> findByBatchIdOrderByCreatedAtDesc(Long batchId);
    
    List<VendorPayoutHistory> findByBatchDateAndPayoutStatusOrderByCreatedAtDesc(LocalDate batchDate, String payoutStatus);
}

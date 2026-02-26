package com.farmeazy.repository;

import com.farmeazy.entity.ServiceBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, Long> {
    Page<ServiceBooking> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT sb FROM ServiceBooking sb WHERE sb.serviceListing.user.id = :providerId")
    Page<ServiceBooking> findByProviderId(@Param("providerId") Long providerId, Pageable pageable);
}

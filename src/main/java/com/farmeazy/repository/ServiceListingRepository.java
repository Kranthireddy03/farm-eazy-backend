package com.farmeazy.repository;

import com.farmeazy.entity.ServiceListing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceListingRepository extends JpaRepository<ServiceListing, Long> {
    List<ServiceListing> findByUserId(Long userId);
    org.springframework.data.domain.Page<ServiceListing> findByUserId(Long userId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<ServiceListing> findByUserIdNot(Long userId, org.springframework.data.domain.Pageable pageable);
}

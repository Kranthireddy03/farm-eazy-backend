package com.farmeazy.repository;

import com.farmeazy.entity.ServiceAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceAttributeRepository extends JpaRepository<ServiceAttribute, Long> {

    List<ServiceAttribute> findByServiceListingId(Long serviceListingId);

    List<ServiceAttribute> findByServiceListingIdOrderByDisplayOrderAsc(Long serviceListingId);

    void deleteByServiceListingId(Long serviceListingId);

    List<ServiceAttribute> findByServiceListingIdAndAttributeKey(Long serviceListingId, String attributeKey);
}

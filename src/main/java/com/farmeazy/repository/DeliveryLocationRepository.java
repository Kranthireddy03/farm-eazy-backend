package com.farmeazy.repository;

import com.farmeazy.entity.DeliveryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryLocationRepository extends JpaRepository<DeliveryLocation, Long> {
    List<DeliveryLocation> findByActiveTrueOrderByLocationNameAsc();

    Optional<DeliveryLocation> findByIdAndActiveTrue(Long id);

    List<DeliveryLocation> findByPostalCodeIgnoreCaseAndActiveTrue(String postalCode);
}
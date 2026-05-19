package com.farmeazy.repository;

import com.farmeazy.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {
    List<Farm> findByUserId(Long userId);
    Optional<Farm> findByIdAndUserId(Long id, Long userId);
    Page<Farm> findByUserId(Long userId, Pageable pageable);
    Page<Farm> findByUserIdAndFarmNameContainingIgnoreCase(Long userId, String farmName, Pageable pageable);
}

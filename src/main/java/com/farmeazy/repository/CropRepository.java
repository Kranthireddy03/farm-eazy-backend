package com.farmeazy.repository;

import com.farmeazy.entity.Crop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CropRepository extends JpaRepository<Crop, Long> {
    List<Crop> findByFarmId(Long farmId);
    List<Crop> findByFarmUserId(Long userId);
    Optional<Crop> findByIdAndFarmUserId(Long id, Long userId);
    Page<Crop> findByFarmId(Long farmId, Pageable pageable);
    Page<Crop> findByFarmIdAndCropNameContainingIgnoreCase(Long farmId, String cropName, Pageable pageable);
}

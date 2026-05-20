package com.farmeazy.repository;

import com.farmeazy.entity.Crop;
import com.farmeazy.entity.IrrigationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IrrigationScheduleRepository extends JpaRepository<IrrigationSchedule, Long> {
    List<IrrigationSchedule> findByCropId(Long cropId);
    List<IrrigationSchedule> findByFarmId(Long farmId);
    Page<IrrigationSchedule> findByFarmId(Long farmId, Pageable pageable);
    List<IrrigationSchedule> findByFarmIdAndStatus(Long farmId, String status);
    List<IrrigationSchedule> findByIrrigationDateAfter(LocalDate date);
    List<IrrigationSchedule> findByFarmIdAndIrrigationDateAfter(Long farmId, LocalDate date);
    Optional<IrrigationSchedule> findByIdAndFarmUserId(Long id, Long userId);
    List<IrrigationSchedule> findByFarmIdAndIrrigationDateBetween(Long farmId, LocalDate startDate, LocalDate endDate);

    /**
     * Find the most recent completed irrigation for a crop (for smart recommendations)
     */
    @Query("SELECT i FROM IrrigationSchedule i WHERE i.crop = :crop AND i.status = 'COMPLETED' " +
            "ORDER BY i.completedAt DESC LIMIT 1")
    Optional<IrrigationSchedule> findTopByCropAndStatusOrderByLastIrrigationDateDesc(
            @Param("crop") Crop crop
    );

    @Query("SELECT i FROM IrrigationSchedule i WHERE i.farm.id = :farmId " +
            "AND i.nextIrrigationDate BETWEEN :startDate AND :endDate " +
            "AND (i.active = true OR i.active IS NULL) " +
            "AND i.status IN ('SCHEDULED', 'PENDING') " +
            "ORDER BY i.nextIrrigationDate ASC")
    List<IrrigationSchedule> findDueIrrigations(
            @Param("farmId") Long farmId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

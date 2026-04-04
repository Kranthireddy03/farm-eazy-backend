package com.farmeazy.repository;

import com.farmeazy.entity.IrrigationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for IrrigationHistory entity
 * Tracks actual irrigation events performed by farmers
 */
@Repository
public interface IrrigationHistoryRepository extends JpaRepository<IrrigationHistory, Long> {

    /**
     * Find irrigation history for a farm within date range
     */
    @Query("SELECT ih FROM IrrigationHistory ih " +
           "WHERE ih.farmId = :farmId " +
           "AND ih.actualIrrigationDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ih.actualIrrigationDate DESC")
    List<IrrigationHistory> findByFarmIdAndDateRange(
        @Param("farmId") Long farmId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find irrigation history for specific crop and farm within date range
     */
    @Query("SELECT ih FROM IrrigationHistory ih " +
           "WHERE ih.farmId = :farmId " +
           "AND ih.cropId = :cropId " +
           "AND ih.actualIrrigationDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ih.actualIrrigationDate DESC")
    List<IrrigationHistory> findByFarmIdAndCropIdAndDateRange(
        @Param("farmId") Long farmId,
        @Param("cropId") Long cropId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all irrigation history for a crop
     */
    List<IrrigationHistory> findByCropId(Long cropId);

    /**
     * Count irrigations for a farm in a month
     */
    @Query("SELECT COUNT(ih) FROM IrrigationHistory ih " +
           "WHERE ih.farmId = :farmId " +
           "AND YEAR(ih.actualIrrigationDate) = :year " +
           "AND MONTH(ih.actualIrrigationDate) = :month")
    Long countByFarmIdAndMonth(
        @Param("farmId") Long farmId,
        @Param("year") Integer year,
        @Param("month") Integer month
    );

    /**
     * Calculate average water efficiency for farm
     */
    @Query("SELECT AVG(ih.waterEfficiencyPercentage) FROM IrrigationHistory ih " +
           "WHERE ih.farmId = :farmId " +
           "AND ih.actualIrrigationDate >= :startDate")
    Double getAverageEfficiency(
        @Param("farmId") Long farmId,
        @Param("startDate") LocalDate startDate
    );

    /**
     * Get total water used in period
     */
    @Query("SELECT COALESCE(SUM(ih.actualWaterUsedMm), 0.0) FROM IrrigationHistory ih " +
           "WHERE ih.farmId = :farmId " +
           "AND ih.actualIrrigationDate BETWEEN :startDate AND :endDate")
    Double getTotalWaterUsed(
        @Param("farmId") Long farmId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}

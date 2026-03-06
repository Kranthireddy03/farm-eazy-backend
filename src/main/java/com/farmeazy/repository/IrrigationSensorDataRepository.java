package com.farmeazy.repository;

import com.farmeazy.entity.IrrigationSensorData;
import com.farmeazy.entity.IrrigationSensorData.SensorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * IRRIGATION SENSOR DATA REPOSITORY
 * 
 * PURPOSE: Data access layer for sensor readings.
 * Provides methods to query and analyze irrigation sensor data.
 */
@Repository
public interface IrrigationSensorDataRepository extends JpaRepository<IrrigationSensorData, Long> {

    /**
     * Find by farm ID with pagination.
     */
    Page<IrrigationSensorData> findByFarmIdOrderByReadingTimestampDesc(Long farmId, Pageable pageable);

    /**
     * Find by farm and sensor type.
     */
    List<IrrigationSensorData> findByFarmIdAndSensorTypeOrderByReadingTimestampDesc(
            Long farmId, SensorType sensorType);

    /**
     * Find by sensor ID.
     */
    Page<IrrigationSensorData> findBySensorIdOrderByReadingTimestampDesc(String sensorId, Pageable pageable);

    /**
     * Find latest reading by sensor.
     */
    Optional<IrrigationSensorData> findTopBySensorIdOrderByReadingTimestampDesc(String sensorId);

    /**
     * Find latest readings by farm and type.
     */
    @Query("SELECT s FROM IrrigationSensorData s WHERE s.farm.id = :farmId " +
           "AND s.sensorType = :sensorType " +
           "AND s.readingTimestamp = (SELECT MAX(s2.readingTimestamp) FROM IrrigationSensorData s2 " +
           "WHERE s2.sensorId = s.sensorId)")
    List<IrrigationSensorData> findLatestReadingsByFarmAndType(
            @Param("farmId") Long farmId,
            @Param("sensorType") SensorType sensorType);

    /**
     * Find unprocessed readings for automation.
     */
    @Query("SELECT s FROM IrrigationSensorData s WHERE s.processed = false " +
           "ORDER BY s.readingTimestamp ASC")
    List<IrrigationSensorData> findUnprocessedReadings();

    /**
     * Find anomalies for review.
     */
    List<IrrigationSensorData> findByIsAnomalyTrueOrderByReadingTimestampDesc();

    /**
     * Calculate average reading.
     */
    @Query("SELECT AVG(s.readingValue) FROM IrrigationSensorData s " +
           "WHERE s.farm.id = :farmId AND s.sensorType = :sensorType " +
           "AND s.readingTimestamp BETWEEN :start AND :end")
    BigDecimal calculateAverageReading(
            @Param("farmId") Long farmId,
            @Param("sensorType") SensorType sensorType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find readings in date range.
     */
    List<IrrigationSensorData> findByFarmIdAndReadingTimestampBetweenOrderByReadingTimestampAsc(
            Long farmId, LocalDateTime start, LocalDateTime end);
}

package com.farmeazy.repository;

import com.farmeazy.entity.IrrigationAutomationRule;
import com.farmeazy.entity.IrrigationSensorData.SensorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * IRRIGATION AUTOMATION RULE REPOSITORY
 * 
 * PURPOSE: Data access layer for irrigation automation rules.
 * Provides methods to query and manage automation triggers.
 */
@Repository
public interface IrrigationAutomationRuleRepository extends JpaRepository<IrrigationAutomationRule, Long> {

    /**
     * Find active rules by farm.
     */
    List<IrrigationAutomationRule> findByFarmIdAndIsActiveTrueOrderByPriorityAsc(Long farmId);

    /**
     * Find active rules by sensor type.
     */
    List<IrrigationAutomationRule> findBySensorTypeAndIsActiveTrueOrderByPriorityAsc(SensorType sensorType);

    /**
     * Find active rules by farm and sensor type.
     */
    @Query("SELECT r FROM IrrigationAutomationRule r WHERE r.farm.id = :farmId " +
           "AND r.sensorType = :sensorType AND r.isActive = true " +
           "ORDER BY r.priority ASC")
    List<IrrigationAutomationRule> findActiveRulesForSensor(
            @Param("farmId") Long farmId,
            @Param("sensorType") SensorType sensorType);

    /**
     * Find all rules by farm.
     */
    List<IrrigationAutomationRule> findByFarmIdOrderByPriorityAsc(Long farmId);

    /**
     * Find rules by crop.
     */
    List<IrrigationAutomationRule> findByCropIdOrderByPriorityAsc(Long cropId);

    /**
     * Count active rules by farm.
     */
    long countByFarmIdAndIsActiveTrue(Long farmId);
}

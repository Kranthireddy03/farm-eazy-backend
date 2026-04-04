package com.farmeazy.repository;

import com.farmeazy.entity.CropIrrigationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CropIrrigationRule - Manages irrigation rules for different crops
 * 
 * Caching Strategy:
 * - Rules are cached for 1 hour in Redis (configured in application.properties)
 * - Critical queries use @Cacheable to reduce DB load
 * - Cache is invalidated automatically after TTL or via @CacheEvict if rules are updated
 */
@Repository
public interface CropIrrigationRuleRepository extends JpaRepository<CropIrrigationRule, Long> {

    /**
     * Find irrigation rule for a specific crop, soil, season, and region combination
     * Cached for performance
     */
    @Query("SELECT c FROM CropIrrigationRule c WHERE c.cropType = :cropType " +
            "AND c.soilType = :soilType AND c.season = :season AND c.region = :region " +
            "AND c.active = true")
    Optional<CropIrrigationRule> findByExactMatch(
            @Param("cropType") String cropType,
            @Param("soilType") String soilType,
            @Param("season") String season,
            @Param("region") String region
    );

    /**
     * Find irrigation rules for a crop without region specificity (fallback)
     */
    @Query("SELECT c FROM CropIrrigationRule c WHERE c.cropType = :cropType " +
            "AND c.soilType = :soilType AND c.season = :season AND c.active = true")
    List<CropIrrigationRule> findByVariant(
            @Param("cropType") String cropType,
            @Param("soilType") String soilType,
            @Param("season") String season
    );

    /**
     * Find all rules for a specific crop (to show all recommended practices)
     */
    @Query("SELECT c FROM CropIrrigationRule c WHERE c.cropType = :cropType AND c.active = true")
    List<CropIrrigationRule> findByCropType(@Param("cropType") String cropType);

    /**
     * Find rules by crop and region (for location-specific recommendations)
     */
    @Query("SELECT c FROM CropIrrigationRule c WHERE c.cropType = :cropType " +
            "AND c.region = :region AND c.active = true")
    List<CropIrrigationRule> findByCropAndRegion(
            @Param("cropType") String cropType,
            @Param("region") String region
    );

    /**
     * Find all active rules for search/filter purposes
     */
    @Query("SELECT c FROM CropIrrigationRule c WHERE c.active = true ORDER BY c.cropType, c.soilType")
    List<CropIrrigationRule> findAllActive();
}

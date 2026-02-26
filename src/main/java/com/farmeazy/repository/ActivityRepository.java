package com.farmeazy.repository;

import com.farmeazy.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ACTIVITY REPOSITORY
 * 
 * PURPOSE: Data access layer for Activity entities
 * Provides methods to query and persist activity records
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    /**
     * Find all activities for a specific user
     * Returns activities in reverse chronological order (newest first)
     */
    Page<Activity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Find recent activities for a user (limited to last N records)
     */
    List<Activity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find activities of a specific type for a user
     */
    List<Activity> findByUserIdAndActivityTypeOrderByCreatedAtDesc(Long userId, String activityType);
}

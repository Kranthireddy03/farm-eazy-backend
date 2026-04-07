package com.farmeazy.repository;

import com.farmeazy.entity.FraudDetectionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudDetectionLogRepository extends JpaRepository<FraudDetectionLog, Long> {
    
    List<FraudDetectionLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<FraudDetectionLog> findByRiskLevelOrderByCreatedAtDesc(String riskLevel);
    
    List<FraudDetectionLog> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT f FROM FraudDetectionLog f WHERE f.userId = :userId AND f.createdAt >= :since ORDER BY f.createdAt DESC")
    List<FraudDetectionLog> findRecentFraudLogsForUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(f) FROM FraudDetectionLog f WHERE f.userId = :userId AND f.riskLevel = 'HIGH' AND f.createdAt >= :since")
    Long countHighRiskEventsForUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}

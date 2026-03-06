package com.farmeazy.repository;

import com.farmeazy.entity.BatchJobExecution;
import com.farmeazy.entity.BatchJobExecution.JobStatus;
import com.farmeazy.entity.BatchJobExecution.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * BATCH JOB EXECUTION REPOSITORY
 * 
 * PURPOSE: Data access layer for batch job execution tracking.
 * Provides methods to query job history, status, and analytics.
 */
@Repository
public interface BatchJobExecutionRepository extends JpaRepository<BatchJobExecution, Long> {

    /**
     * Find jobs by type and status.
     */
    List<BatchJobExecution> findByJobTypeAndJobStatus(JobType jobType, JobStatus jobStatus);

    /**
     * Find jobs by status.
     */
    List<BatchJobExecution> findByJobStatus(JobStatus jobStatus);

    /**
     * Find recent jobs with pagination.
     */
    Page<BatchJobExecution> findByOrderByStartTimeDesc(Pageable pageable);

    /**
     * Find recent failed jobs for analysis.
     */
    @Query("SELECT b FROM BatchJobExecution b WHERE b.jobStatus IN ('FAILED', 'PARTIALLY_COMPLETED') " +
           "AND b.startTime >= :since ORDER BY b.startTime DESC")
    List<BatchJobExecution> findRecentFailedJobs(@Param("since") LocalDateTime since);

    /**
     * Find jobs by type within date range.
     */
    @Query("SELECT b FROM BatchJobExecution b WHERE b.jobType = :jobType " +
           "AND b.startTime BETWEEN :startDate AND :endDate ORDER BY b.startTime DESC")
    List<BatchJobExecution> findByJobTypeAndDateRange(
            @Param("jobType") JobType jobType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find the last execution of a specific job type.
     */
    Optional<BatchJobExecution> findTopByJobTypeOrderByStartTimeDesc(JobType jobType);

    /**
     * Count jobs by status for dashboard.
     */
    @Query("SELECT b.jobStatus, COUNT(b) FROM BatchJobExecution b " +
           "WHERE b.startTime >= :since GROUP BY b.jobStatus")
    List<Object[]> countJobsByStatus(@Param("since") LocalDateTime since);

    /**
     * Find currently running jobs.
     */
    @Query("SELECT b FROM BatchJobExecution b WHERE b.jobStatus IN ('STARTED', 'RUNNING')")
    List<BatchJobExecution> findRunningJobs();
}

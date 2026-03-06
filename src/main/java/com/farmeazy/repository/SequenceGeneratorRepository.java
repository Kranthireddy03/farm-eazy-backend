package com.farmeazy.repository;

import com.farmeazy.entity.IdSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * SEQUENCE GENERATOR REPOSITORY
 * 
 * PURPOSE: Data access layer for sequential ID generation.
 * Provides thread-safe methods for generating unique IDs.
 */
@Repository
public interface SequenceGeneratorRepository extends JpaRepository<IdSequence, Long> {

    /**
     * Find sequence by name.
     */
    Optional<IdSequence> findBySequenceName(String sequenceName);

    /**
     * Find sequence by name with pessimistic lock for thread-safe increment.
     * This ensures no two threads get the same ID.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM IdSequence s WHERE s.sequenceName = :name")
    Optional<IdSequence> findBySequenceNameWithLock(@Param("name") String name);

    /**
     * Check if sequence exists.
     */
    boolean existsBySequenceName(String sequenceName);
}

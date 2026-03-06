package com.farmeazy.repository;

import com.farmeazy.entity.CommunicationPreference;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * COMMUNICATION PREFERENCE REPOSITORY
 * 
 * PURPOSE: Data access layer for user communication preferences.
 */
@Repository
public interface CommunicationPreferenceRepository extends JpaRepository<CommunicationPreference, Long> {
    
    Optional<CommunicationPreference> findByUser(User user);
    
    Optional<CommunicationPreference> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}

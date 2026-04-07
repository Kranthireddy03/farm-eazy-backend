package com.farmeazy.repository;

import com.farmeazy.entity.PayoutConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayoutConfigRepository extends JpaRepository<PayoutConfig, Long> {

    /**
     * Get config by key (case-sensitive)
     */
    Optional<PayoutConfig> findByConfigKey(String configKey);

    /**
     * Check if config is active
     */
    boolean existsByConfigKeyAndIsActive(String configKey, boolean isActive);
}

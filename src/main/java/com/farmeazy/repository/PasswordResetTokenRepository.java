package com.farmeazy.repository;

import com.farmeazy.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByShortCode(String shortCode);
    Optional<PasswordResetToken> findByFullTokenAndUsedFalse(String fullToken);
    
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    
    void deleteByEmail(String email);
}

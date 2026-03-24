package com.farmeazy.repository;

import com.farmeazy.entity.RefreshToken;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    long deleteByUser(User user);

    long deleteByExpiresAtBefore(LocalDateTime now);
}

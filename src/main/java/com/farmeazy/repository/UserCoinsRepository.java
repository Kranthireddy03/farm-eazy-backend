package com.farmeazy.repository;

import com.farmeazy.entity.User;
import com.farmeazy.entity.UserCoins;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCoinsRepository extends JpaRepository<UserCoins, Long> {
    Optional<UserCoins> findByUser(User user);
    Optional<UserCoins> findByUserId(Long userId);
}

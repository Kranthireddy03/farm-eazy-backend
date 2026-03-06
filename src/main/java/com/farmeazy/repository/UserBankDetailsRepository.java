package com.farmeazy.repository;

import com.farmeazy.entity.User;
import com.farmeazy.entity.UserBankDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBankDetailsRepository extends JpaRepository<UserBankDetails, Long> {

    Optional<UserBankDetails> findByUserId(Long userId);

    Optional<UserBankDetails> findByUser(User user);

    boolean existsByUserId(Long userId);

    Optional<UserBankDetails> findByUserIdAndIsPrimaryTrue(Long userId);

    Optional<UserBankDetails> findByUserIdAndIsVerifiedTrue(Long userId);
}

package com.farmeazy.repository;

import com.farmeazy.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    
    Optional<OtpVerification> findByEmailAndOtpCodeAndPurpose(String email, String otpCode, String purpose);
    
    Optional<OtpVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, String purpose);
    
    // Phone-based OTP methods (for OTP login)
    Optional<OtpVerification> findByPhoneAndOtpCodeAndPurpose(String phone, String otpCode, String purpose);
    
    Optional<OtpVerification> findTopByPhoneAndPurposeOrderByCreatedAtDesc(String phone, String purpose);
    
    void deleteByExpiresAtBefore(LocalDateTime now);
}

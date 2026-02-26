package com.farmeazy.repository;

import com.farmeazy.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Custom queries if needed
}

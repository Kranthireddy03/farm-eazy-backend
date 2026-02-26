package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String orderId;
    private Double amount;
    private String status;
    private String transactionId;
    private String email;
    private String phone;
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    // ...
}

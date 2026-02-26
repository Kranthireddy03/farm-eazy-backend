package com.farmeazy.repository;

import com.farmeazy.entity.Order;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    // List<Order> findBySellerOrderByCreatedAtDesc(User seller);
    Optional<Order> findByIdAndUser(Long id, User user);
    Long countByUser(User user);
}

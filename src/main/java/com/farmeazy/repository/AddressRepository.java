package com.farmeazy.repository;

import com.farmeazy.entity.Address;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserOrderByCreatedAtDesc(User user);
    Optional<Address> findByIdAndUser(Long id, User user);
    Optional<Address> findByUserAndIsDefaultTrue(User user);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT a.state) FROM Address a WHERE a.state IS NOT NULL")
    long countDistinctStates();
}

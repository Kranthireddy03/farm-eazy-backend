package com.farmeazy.repository;

import com.farmeazy.entity.UserActivity;
import com.farmeazy.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    Page<UserActivity> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    List<UserActivity> findTop20ByUserOrderByCreatedAtDesc(User user);
    Long countByUser(User user);
}

package com.farmeazy.repository;

import com.farmeazy.entity.RoleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoleHistoryRepository extends JpaRepository<RoleHistory, Long> {
    List<RoleHistory> findByRoleIdOrderByChangedAtDesc(Long roleId);
}

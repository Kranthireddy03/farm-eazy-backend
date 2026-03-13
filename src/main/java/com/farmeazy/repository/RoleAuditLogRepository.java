package com.farmeazy.repository;

import com.farmeazy.entity.RoleAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleAuditLogRepository extends JpaRepository<RoleAuditLog, Long> {
}

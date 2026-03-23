package com.farmeazy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_history")
public class RoleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roleId;
    private String roleName;

    @Lob
    private String permissionsSnapshot;

    private String changedBy;

    private LocalDateTime changedAt;

    public RoleHistory() {}

    public RoleHistory(Long roleId, String roleName, String permissionsSnapshot, String changedBy) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.permissionsSnapshot = permissionsSnapshot;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getPermissionsSnapshot() {
        return permissionsSnapshot;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}

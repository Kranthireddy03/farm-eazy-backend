package com.farmeazy.controller;

import com.farmeazy.entity.RoleAuditLog;
import com.farmeazy.entity.RoleEntity;
import com.farmeazy.entity.RoleHistory;
import com.farmeazy.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "Admin Role Management", description = "Manage roles, history, and audit logs")
public class AdminRoleController {

    @Autowired
    private RoleService roleService;

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<List<RoleEntity>> listRoles() {
        return ResponseEntity.ok(roleService.getRoles());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<RoleEntity> createRole(@RequestBody RoleEntity role) {
        return ResponseEntity.ok(roleService.createRole(role, getCurrentUsername()));
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<RoleEntity> updateRole(@PathVariable Long roleId, @RequestBody RoleEntity role) {
        return ResponseEntity.ok(roleService.updateRole(roleId, role, getCurrentUsername()));
    }

    @GetMapping("/{roleId}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<List<RoleHistory>> getRoleHistory(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getRoleHistory(roleId));
    }

    @PostMapping("/revert/{historyId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<RoleEntity> revertRole(@PathVariable Long historyId) {
        return ResponseEntity.ok(roleService.revertRole(historyId, getCurrentUsername()));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<List<RoleAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(roleService.findAuditLogs());
    }
}

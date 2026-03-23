package com.farmeazy.controller;

import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "Admin User Role Management", description = "Manage user roles (admin only)")
public class AdminUserRoleController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.farmeazy.service.RoleService roleService;

    @PutMapping("/{email}/role")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    @Operation(summary = "Assign role to user", description = "Add or update a role for a user by email")
    public ResponseEntity<?> assignRole(@PathVariable String email, @RequestParam String role) {
        // New behavior: log assignment and keep immutable audit history via RoleAuditLog
        roleService.assignUserRole(email, role, SecurityContextHolder.getContext().getAuthentication().getName());
        return ResponseEntity.ok("Role assigned: " + role);
    }
}

package com.farmeazy.controller;

import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PutMapping("/{email}/role")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    @Operation(summary = "Assign role to user", description = "Add or update a role for a user by email")
    public ResponseEntity<?> assignRole(@PathVariable String email, @RequestParam String role) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        Set<String> roles = user.getRoles() != null ? user.getRoles() : new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok("Role assigned: " + role);
    }
}

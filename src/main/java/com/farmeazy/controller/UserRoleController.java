package com.farmeazy.controller;

import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.entity.RoleAuditLog;
import com.farmeazy.repository.RoleAuditLogRepository;
import com.farmeazy.service.EmailService;
import com.farmeazy.service.EmailType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/admin/users")
public class UserRoleController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleAuditLogRepository roleAuditLogRepository;

    @Autowired
    private EmailService emailService;

    @PutMapping("/id/{userId}/role")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<User> setRoleById(@PathVariable Long userId, @RequestParam String role, @RequestParam boolean assign, @RequestParam(required = false) String reason) {
        // Validate allowed roles
        java.util.Set<String> allowed = java.util.Set.of("SUPERADMIN","ADMIN","USER");
        if (role == null || !allowed.contains(role)) {
            return ResponseEntity.badRequest().build();
        }
        Optional<User> u = userRepository.findById(userId);
        if (u.isEmpty()) return ResponseEntity.notFound().build();
        User user = u.get();
        Set<String> roles = user.getRoles() == null ? new HashSet<>() : user.getRoles();
        boolean alreadyHasRole = roles.contains(role);
        if (assign) roles.add(role);
        else roles.remove(role);
        user.setRoles(roles);
        userRepository.save(user);

        // Audit log
        String changedBy = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        RoleAuditLog log = new RoleAuditLog(user.getEmail(), user.getId(), role, assign, changedBy, reason);
        roleAuditLogRepository.save(log);

        // Email notification
        String subject = assign ? "Role Assigned: " + role : "Role Removed: " + role;
        String action = assign ? "assigned" : "removed";
        String message = String.format("Your role '%s' was %s by %s. Reason: %s", role, action, changedBy, reason == null ? "-" : reason);
        emailService.sendNotificationEmail(user.getEmail(), user.getUsername(), subject, message, EmailType.SUPPORT);

        return ResponseEntity.ok(user);
    }
        @GetMapping("/id/{userId}/roles")
        @PreAuthorize("hasRole('SUPERADMIN')")
        public ResponseEntity<Set<String>> getRolesById(@PathVariable Long userId) {
            Optional<User> u = userRepository.findById(userId);
            if (u.isEmpty()) return ResponseEntity.notFound().build();
            User user = u.get();
            Set<String> roles = user.getRoles() == null ? new HashSet<>() : user.getRoles();
            return ResponseEntity.ok(roles);
        }

        @GetMapping("/{email}/roles")
        @PreAuthorize("hasRole('SUPERADMIN')")
        public ResponseEntity<Set<String>> getRolesByEmail(@PathVariable String email) {
            Optional<User> u = userRepository.findByEmail(email);
            if (u.isEmpty()) return ResponseEntity.notFound().build();
            User user = u.get();
            Set<String> roles = user.getRoles() == null ? new HashSet<>() : user.getRoles();
            return ResponseEntity.ok(roles);
        }

    @PutMapping("/email/{email}/role")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<User> setRoleByEmail(@PathVariable String email, @RequestParam String role, @RequestParam boolean assign, @RequestParam(required = false) String reason) {
        // Validate allowed roles
        java.util.Set<String> allowed = java.util.Set.of("SUPERADMIN","ADMIN","USER");
        if (role == null || !allowed.contains(role)) {
            return ResponseEntity.badRequest().build();
        }
        Optional<User> u = userRepository.findByEmail(email);
        if (u.isEmpty()) return ResponseEntity.notFound().build();
        User user = u.get();
        Set<String> roles = user.getRoles() == null ? new HashSet<>() : user.getRoles();
        boolean alreadyHasRole = roles.contains(role);
        if (assign) roles.add(role);
        else roles.remove(role);
        user.setRoles(roles);
        userRepository.save(user);

        // Audit log
        String changedBy = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        RoleAuditLog log = new RoleAuditLog(user.getEmail(), user.getId(), role, assign, changedBy, reason);
        roleAuditLogRepository.save(log);

        // Email notification
        String subject = assign ? "Role Assigned: " + role : "Role Removed: " + role;
        String action = assign ? "assigned" : "removed";
        String message = String.format("Your role '%s' was %s by %s. Reason: %s", role, action, changedBy, reason == null ? "-" : reason);
        emailService.sendNotificationEmail(user.getEmail(), user.getUsername(), subject, message, EmailType.SUPPORT);

        return ResponseEntity.ok(user);
    }
}

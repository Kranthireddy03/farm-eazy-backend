package com.farmeazy.service;

import com.farmeazy.entity.RoleAuditLog;
import com.farmeazy.entity.RoleEntity;
import com.farmeazy.entity.RoleHistory;
import com.farmeazy.entity.User;
import com.farmeazy.entity.Notification.NotificationPriority;
import com.farmeazy.entity.Notification.NotificationType;
import com.farmeazy.repository.RoleAuditLogRepository;
import com.farmeazy.repository.RoleHistoryRepository;
import com.farmeazy.repository.RoleRepository;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RoleHistoryRepository roleHistoryRepository;

    @Autowired
    private RoleAuditLogRepository roleAuditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    private final ObjectMapper mapper = new ObjectMapper();

    public List<RoleEntity> getRoles() {
        return roleRepository.findAll();
    }

    public RoleEntity createRole(RoleEntity role, String currentUser) {
        RoleEntity saved = roleRepository.save(role);
        saveHistory(saved, currentUser);
        return saved;
    }

    @Transactional
    public RoleEntity updateRole(Long roleId, RoleEntity incoming, String currentUser) {
        RoleEntity existing = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        saveHistory(existing, currentUser);
        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setPermissions(incoming.getPermissions());
        RoleEntity updated = roleRepository.save(existing);

        return updated;
    }

    public RoleEntity getRole(Long roleId) {
        return roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    public List<RoleHistory> getRoleHistory(Long roleId) {
        return roleHistoryRepository.findByRoleIdOrderByChangedAtDesc(roleId);
    }

    @Transactional
    public RoleEntity revertRole(Long historyId, String currentUser) {
        RoleHistory history = roleHistoryRepository.findById(historyId).orElseThrow(() -> new ResourceNotFoundException("RoleHistory not found"));
        RoleEntity role = getRole(history.getRoleId());
        try {
            Set<String> permissions = mapper.readValue(history.getPermissionsSnapshot(), Set.class);
            role.setPermissions(permissions);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse permissions snapshot", e);
        }
        RoleEntity updated = roleRepository.save(role);
        saveHistory(updated, currentUser);
        return updated;
    }

    private void saveHistory(RoleEntity role, String changedBy) {
        try {
            String snapshot = mapper.writeValueAsString(role.getPermissions());
            RoleHistory history = new RoleHistory(role.getId(), role.getName(), snapshot, changedBy);
            roleHistoryRepository.save(history);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize role permissions", e);
        }
    }

    public List<RoleAuditLog> findAuditLogs() {
        return roleAuditLogRepository.findAll();
    }

    public void assignUserRole(String email, String roleName, String changedBy) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Set<String> roles = user.getRoles();
        if (roles == null) {
            roles = Set.of(roleName);
        } else {
            roles.add(roleName);
        }
        user.setRoles(roles);
        userRepository.save(user);

        RoleAuditLog log = new RoleAuditLog(user.getEmail(), user.getId(), roleName, true, changedBy, "Assigned role");
        roleAuditLogRepository.save(log);

    notificationService.createForUser(
        user,
        NotificationType.ACCOUNT,
        "Role updated: " + roleName,
        "An administrator updated your account role to " + roleName + ".",
        "/user/profile",
        NotificationPriority.HIGH
    );
    }

}

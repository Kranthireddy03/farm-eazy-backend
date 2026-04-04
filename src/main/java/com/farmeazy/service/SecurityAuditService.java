package com.farmeazy.service;

import com.farmeazy.entity.SystemAuditLog;
import com.farmeazy.entity.User;
import com.farmeazy.repository.SystemAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecurityAuditService {

    private final SystemAuditLogRepository systemAuditLogRepository;

    @Autowired
    public SecurityAuditService(SystemAuditLogRepository systemAuditLogRepository) {
        this.systemAuditLogRepository = systemAuditLogRepository;
    }

    public void logRefundAction(User user, String actionDescription, boolean success, String additionalInfo) {
        logAction(SystemAuditLog.EventType.REFUND_INITIATED, user, actionDescription, "REFUND_DETAILS",
                success ? 200 : 400, success ? 15 : 70, additionalInfo);
    }

    public void logBankAction(User user, String actionDescription, boolean success, String additionalInfo) {
        logAction(SystemAuditLog.EventType.BANK_DETAILS_CHANGED, user, actionDescription, "BANK_DETAILS",
                success ? 200 : 400, success ? 10 : 65, additionalInfo);
    }

    private void logAction(SystemAuditLog.EventType eventType,
                           User user,
                           String actionDescription,
                           String resourceType,
                           Integer responseStatus,
                           int riskScore,
                           String additionalInfo) {
        try {
            SystemAuditLog log = SystemAuditLog.builder()
                    .eventType(eventType)
                    .user(user)
                    .action(actionDescription)
                    .resource(resourceType, user != null && user.getId() != null ? String.valueOf(user.getId()) : null)
                    .responseStatus(responseStatus)
                    .suspicious(riskScore >= 60, riskScore)
                    .additionalInfo(additionalInfo)
                    .build();
            systemAuditLogRepository.save(log);
        } catch (Exception ignored) {
            // Audit logging must never block business operations.
        }
    }
}

package com.farmeazy.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class StartupDatabaseFix {

    private static final Logger logger = LoggerFactory.getLogger(StartupDatabaseFix.class);

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${superadmin.default.password:}")
    private String superadminPassword;

    private static final String SUPERADMIN_EMAIL = "support@farm-eazy.com";
    private static final String SUPERADMIN_USERNAME = "support";
    private static final String SUPERADMIN_PHONE = "6301630368";

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        ensureSuperadminUser();
    }

    

    public void ensureSuperadminUser() {
        try {
            jdbc.update("UPDATE users SET profile_completed = TRUE WHERE profile_completed IS NULL");

            if (superadminPassword == null || superadminPassword.isBlank()) {
                logger.warn("[StartupDatabaseFix] SUPERADMIN_DEFAULT_PASSWORD not set; skipping support@farm-eazy.com bootstrap");
                return;
            }
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", new Object[]{SUPERADMIN_EMAIL}, Integer.class);
            if (count == null || count == 0) {
                // New superadmin: check if phone is already taken by another user
                Integer phoneCount = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE phone = ?", new Object[]{SUPERADMIN_PHONE}, Integer.class);
                String hashed = new BCryptPasswordEncoder().encode(superadminPassword);
                
                if (phoneCount != null && phoneCount > 0) {
                    logger.warn("[StartupDatabaseFix] Phone {} already taken, inserting superadmin without phone", SUPERADMIN_PHONE);
                    jdbc.update("INSERT INTO users (email, username, password, phone, auth_provider, profile_completed, active, created_at, updated_at) VALUES (?, ?, ?, NULL, 'PASSWORD', TRUE, TRUE, NOW(), NOW())", SUPERADMIN_EMAIL, SUPERADMIN_USERNAME, hashed);
                } else {
                    jdbc.update("INSERT INTO users (email, username, password, phone, auth_provider, profile_completed, active, created_at, updated_at) VALUES (?, ?, ?, ?, 'PASSWORD', TRUE, TRUE, NOW(), NOW())", SUPERADMIN_EMAIL, SUPERADMIN_USERNAME, hashed, SUPERADMIN_PHONE);
                }
                
                Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", new Object[]{SUPERADMIN_EMAIL}, Long.class);
                jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, "SUPERADMIN");
                logger.info("[StartupDatabaseFix] Created default superadmin user: {}", SUPERADMIN_EMAIL);
            } else {
                // Superadmin exists: update phone only if not taken by someone else
                Integer phoneCount = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE phone = ? AND email != ?", new Object[]{SUPERADMIN_PHONE, SUPERADMIN_EMAIL}, Integer.class);
                
                if (phoneCount == null || phoneCount == 0) {
                    jdbc.update("UPDATE users SET phone = ? WHERE email = ? AND (phone IS NULL OR phone = '')", SUPERADMIN_PHONE, SUPERADMIN_EMAIL);
                } else {
                    logger.warn("[StartupDatabaseFix] Phone {} is taken by another user, skipping phone update for superadmin", SUPERADMIN_PHONE);
                }
                
                jdbc.update("UPDATE user_roles SET role = ? WHERE user_id = (SELECT id FROM users WHERE email = ?) AND role = ?", "SUPERADMIN", SUPERADMIN_EMAIL, "ADMIN");
                logger.info("[StartupDatabaseFix] Superadmin user already exists: {}", SUPERADMIN_EMAIL);
            }
        } catch (Exception e) {
            logger.error("[StartupDatabaseFix] Failed to ensure superadmin user", e);
        }
    }
}

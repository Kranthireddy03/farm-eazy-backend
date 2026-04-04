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
            if (superadminPassword == null || superadminPassword.isBlank()) {
                logger.warn("[StartupDatabaseFix] SUPERADMIN_DEFAULT_PASSWORD not set; skipping support@farm-eazy.com bootstrap");
                return;
            }
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", new Object[]{SUPERADMIN_EMAIL}, Integer.class);
            if (count == null || count == 0) {
                String hashed = new BCryptPasswordEncoder().encode(superadminPassword);
                jdbc.update("INSERT INTO users (email, username, password, phone, active, created_at, updated_at) VALUES (?, ?, ?, ?, TRUE, NOW(), NOW())", SUPERADMIN_EMAIL, SUPERADMIN_USERNAME, hashed, SUPERADMIN_PHONE);
                Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", new Object[]{SUPERADMIN_EMAIL}, Long.class);
                jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, "SUPERADMIN");
                logger.info("[StartupDatabaseFix] Created default superadmin user: {}", SUPERADMIN_EMAIL);
            } else {
                jdbc.update("UPDATE users SET phone = ? WHERE email = ? AND (phone IS NULL OR phone = '')", SUPERADMIN_PHONE, SUPERADMIN_EMAIL);
                jdbc.update("UPDATE user_roles SET role = ? WHERE user_id = (SELECT id FROM users WHERE email = ?) AND role = ?", "SUPERADMIN", SUPERADMIN_EMAIL, "ADMIN");
                logger.info("[StartupDatabaseFix] Superadmin user already exists: {}", SUPERADMIN_EMAIL);
            }
        } catch (Exception e) {
            logger.error("[StartupDatabaseFix] Failed to ensure superadmin user", e);
        }
    }
}

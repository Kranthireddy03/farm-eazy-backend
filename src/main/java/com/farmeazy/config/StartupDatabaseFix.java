package com.farmeazy.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StartupDatabaseFix {

    @Autowired
    private JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureNotificationReadColumn() {
        try {
            // try a simple select to see if column exists
            jdbc.queryForObject("SELECT notification_read FROM faqquestion LIMIT 1", Boolean.class);
        } catch (Exception ex) {
            try {
                jdbc.execute("ALTER TABLE faqquestion ADD COLUMN notification_read BOOLEAN DEFAULT FALSE");
                System.out.println("[StartupDatabaseFix] Added column notification_read to faqquestion table");
            } catch (Exception e) {
                System.err.println("[StartupDatabaseFix] Failed to add notification_read column: " + e.getMessage());
            }
        }
    }
}

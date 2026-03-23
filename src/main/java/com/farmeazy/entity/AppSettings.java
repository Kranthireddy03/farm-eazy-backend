package com.farmeazy.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "app_settings")
public class AppSettings {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private String appName = "FarmEazy Support";

    @Column(nullable = false)
    private String supportEmail = "support@farm-eazy.com";

    @Column(nullable = false)
    private String timezone = "UTC";

    // SLA (hours)
    private int slaHigh = 4;
    private int slaMedium = 8;
    private int slaLow = 24;

    // Notification settings
    private boolean emailAlerts = true;
    private boolean slaAlerts = true;
    private boolean ticketAlerts = true;

    // Security settings
    private int sessionTimeoutMinutes = 30;
    private boolean enable2fa = false;

    private java.time.LocalDateTime updatedAt;

    public AppSettings() {
        this.updatedAt = java.time.LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public int getSlaHigh() { return slaHigh; }
    public void setSlaHigh(int slaHigh) { this.slaHigh = slaHigh; }
    public int getSlaMedium() { return slaMedium; }
    public void setSlaMedium(int slaMedium) { this.slaMedium = slaMedium; }
    public int getSlaLow() { return slaLow; }
    public void setSlaLow(int slaLow) { this.slaLow = slaLow; }
    public boolean isEmailAlerts() { return emailAlerts; }
    public void setEmailAlerts(boolean emailAlerts) { this.emailAlerts = emailAlerts; }
    public boolean isSlaAlerts() { return slaAlerts; }
    public void setSlaAlerts(boolean slaAlerts) { this.slaAlerts = slaAlerts; }
    public boolean isTicketAlerts() { return ticketAlerts; }
    public void setTicketAlerts(boolean ticketAlerts) { this.ticketAlerts = ticketAlerts; }
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { this.sessionTimeoutMinutes = sessionTimeoutMinutes; }
    public boolean isEnable2fa() { return enable2fa; }
    public void setEnable2fa(boolean enable2fa) { this.enable2fa = enable2fa; }

    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

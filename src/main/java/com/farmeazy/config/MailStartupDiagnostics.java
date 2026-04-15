package com.farmeazy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class MailStartupDiagnostics {

    private static final Logger logger = LoggerFactory.getLogger(MailStartupDiagnostics.class);

    @Value("${farmeazy.mail.provider:resend}")
    private String mailProvider;

    @Value("${resend.api.key:}")
    private String resendApiKeyProperty;

    private final Environment environment;

    public MailStartupDiagnostics(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logMailStartupStatus() {
        String envResendKey = System.getenv("RESEND_API_KEY");
        boolean hasEnvKey = envResendKey != null && !envResendKey.isBlank();
        boolean hasPropertyKey = resendApiKeyProperty != null && !resendApiKeyProperty.isBlank();

        logger.info("[MailStartupDiagnostics] activeProfiles={}", Arrays.toString(environment.getActiveProfiles()));
        logger.info("[MailStartupDiagnostics] farmeazy.mail.provider={}", mailProvider);
        logger.info("[MailStartupDiagnostics] resend.api.key.property.present={}", hasPropertyKey);
        logger.info("[MailStartupDiagnostics] RESEND_API_KEY.env.present={}", hasEnvKey);

        if ("resend".equalsIgnoreCase(mailProvider) && !hasEnvKey && !hasPropertyKey) {
            logger.warn("[MailStartupDiagnostics] Resend is selected but no API key was found in env/property. Emails will fail.");
        }
    }
}

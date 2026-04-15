package com.farmeazy.service;

import com.farmeazy.dto.ContactMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;

@Service
public class ContactService {
    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    @Autowired
    private com.farmeazy.service.UnifiedEmailService emailService;

    public void processMessage(ContactMessageDto dto) {
        // Log the message
        logger.info("Contact message received from {} <{}>: {}", dto.getName(), dto.getEmail(), dto.getSubject());

        // Send email to support
        String subject = "Contact Form: " + dto.getSubject();
        String html = loadTemplate("contact-form-support.html")
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()))
                .replace("{{NAME}}", safe(dto.getName()))
                .replace("{{EMAIL}}", safe(dto.getEmail()))
                .replace("{{SUBJECT}}", safe(dto.getSubject()))
                .replace("{{MESSAGE}}", safe(dto.getMessage()));
        emailService.sendEmail("support@farm-eazy.com", subject, html, com.farmeazy.service.UnifiedEmailService.SenderType.SUPPORT);
    }

    private String loadTemplate(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/emails/" + fileName);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to load email template: " + fileName, ex);
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

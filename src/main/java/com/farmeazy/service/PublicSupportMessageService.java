package com.farmeazy.service;

import com.farmeazy.dto.PublicSupportMessageDto;
import com.farmeazy.entity.SupportTicket;
import com.farmeazy.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;

@Service
public class PublicSupportMessageService {
    private static final Logger logger = LoggerFactory.getLogger(PublicSupportMessageService.class);

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private UnifiedEmailService emailService;

    public void processPublicMessage(PublicSupportMessageDto dto) {
        // Save as support ticket with source
        SupportTicket ticket = new SupportTicket();
        ticket.setSubject(dto.getSubject());
        ticket.setDescription(dto.getMessage());
        ticket.setContactEmail(dto.getEmail());
        ticket.setDisplayId(null); // Will be generated
        ticket.setCategory(SupportTicket.TicketCategory.FEEDBACK);
        ticket.setPriority(SupportTicket.TicketPriority.MEDIUM);
        ticket.setStatus(SupportTicket.TicketStatus.OPEN);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setSource("SUPPORT_PAGE");
        ticketRepository.save(ticket);

                // Notify admin using template
                String subject = "New Support Message from Public Page: " + dto.getSubject();
                String html = loadTemplate("public-support-admin.html")
                        .replace("{{YEAR}}", String.valueOf(Year.now().getValue()))
                        .replace("{{NAME}}", safe(dto.getName()))
                        .replace("{{EMAIL}}", safe(dto.getEmail()))
                        .replace("{{SUBJECT}}", safe(dto.getSubject()))
                        .replace("{{MESSAGE}}", safe(dto.getMessage()))
                        .replace("{{REPLY_MAILTO}}", "mailto:" + safe(dto.getEmail()));
                emailService.sendEmail("support@farm-eazy.com", subject, html, UnifiedEmailService.SenderType.SUPPORT);

                // Notify user using template
                String userSubject = "Your support request has been received";
                String userHtml = loadTemplate("public-support-user-ack.html")
                        .replace("{{YEAR}}", String.valueOf(Year.now().getValue()))
                        .replace("{{NAME}}", safe(dto.getName()));
                emailService.sendEmail(dto.getEmail(), userSubject, userHtml, UnifiedEmailService.SenderType.SUPPORT);
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

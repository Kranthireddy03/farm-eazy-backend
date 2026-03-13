package com.farmeazy.service;

import com.farmeazy.dto.ContactMessageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        String html = "<h2>Contact Form Submission</h2>" +
            "<b>Name:</b> " + dto.getName() + "<br>" +
            "<b>Email:</b> " + dto.getEmail() + "<br>" +
            "<b>Subject:</b> " + dto.getSubject() + "<br>" +
            "<b>Message:</b><br>" + dto.getMessage() + "<br>";
        emailService.sendEmail("support@farm-eazy.com", subject, html, com.farmeazy.service.UnifiedEmailService.SenderType.SUPPORT);
    }
}

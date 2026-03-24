package com.farmeazy.service;

import com.farmeazy.dto.PublicSupportMessageDto;
import com.farmeazy.entity.SupportTicket;
import com.farmeazy.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

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

                // Notify admin with a professional HTML template
                String subject = "New Support Message from Public Page: " + dto.getSubject();
                String html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset=\"UTF-8\">
                            <title>Support Request Received</title>
                        </head>
                        <body style=\"background:#f4f4f7;padding:0;margin:0;\">
                            <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f4f7;\">
                                <tr>
                                    <td align=\"center\">
                                        <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#fff;border-radius:8px;box-shadow:0 2px 8px #e0e0e0;margin:40px 0;\">
                                            <tr>
                                                <td style=\"background:#4CAF50;padding:24px 0;border-radius:8px 8px 0 0;text-align:center;\">
                                                    <p style="color:#e8f5e9;font-family:sans-serif;margin:6px 0 0 0;font-size:13px;letter-spacing:.3px;">Smart Farm Care Team</p>
                                                    <p style="color:#e8f5e9;font-family:sans-serif;margin:6px 0 0 0;font-size:13px;letter-spacing:.3px;">Smart Farm Care Team</p>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style=\"padding:32px 40px 24px 40px;font-family:sans-serif;color:#333;\">
                                                    <h2 style=\"color:#4CAF50;margin-top:0;\">Support Message Submission</h2>
                                                    <p style=\"font-size:16px;\">You have received a new support request from the public page:</p>
                                                    <table cellpadding=\"0\" cellspacing=\"0\" style=\"margin:24px 0 16px 0;\">
                                                        <tr>
                                                            <td style=\"font-weight:bold;padding:4px 8px 4px 0;\">Name:</td>
                                                            <td style=\"padding:4px 0;\">" + dto.getName() + "</td>
                                                        </tr>
                                                        <tr>
                                                            <td style=\"font-weight:bold;padding:4px 8px 4px 0;\">Email:</td>
                                                            <td style=\"padding:4px 0;\">" + dto.getEmail() + "</td>
                                                        </tr>
                                                        <tr>
                                                            <td style=\"font-weight:bold;padding:4px 8px 4px 0;\">Subject:</td>
                                                            <td style=\"padding:4px 0;\">" + dto.getSubject() + "</td>
                                                        </tr>
                                                        <tr>
                                                            <td style=\"font-weight:bold;padding:4px 8px 4px 0;vertical-align:top;\">Message:</td>
                                                            <td style=\"padding:4px 0;\">" + dto.getMessage() + "</td>
                                                        </tr>
                                                    </table>
                                                    <p style=\"font-size:15px;color:#555;\">Please respond to the user as soon as possible.</p>
                                                    <a href=\"mailto:" + dto.getEmail() + "\" style=\"display:inline-block;margin-top:16px;padding:10px 24px;background:#4CAF50;color:#fff;text-decoration:none;border-radius:4px;font-weight:bold;\">Reply to User</a>
                                                </td>
                                            </tr>
                                                <td style=\"background:#f4f4f7;padding:20px 40px;border-radius:0 0 8px 8px;text-align:center;color:#888;font-size:13px;\">
                                                        <p style="color:#e8f5e9;font-family:sans-serif;margin:6px 0 0 0;font-size:13px;letter-spacing:.3px;">Smart Farm Care Team</p>
                                                    Thank you for using FarmEazy!<br>
                                                    <span style=\"color:#aaa;\">&copy; 2026 FarmEazy. All rights reserved.</span>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </body>
                        </html>
                """;
                emailService.sendEmail("support@farm-eazy.com", subject, html, UnifiedEmailService.SenderType.SUPPORT);

                // Notify user with a professional HTML template
                String userSubject = "Your support request has been received";
                String userHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset=\"UTF-8\">
                            <title>Support Request Received</title>
                        </head>
                        <body style=\"background:#f4f4f7;padding:0;margin:0;\">
                            <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f4f7;\">
                                <tr>
                                    <td align=\"center\">
                                        <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#fff;border-radius:8px;box-shadow:0 2px 8px #e0e0e0;margin:40px 0;\">
                                            <tr>
                                                <td style=\"background:#4CAF50;padding:24px 0;border-radius:8px 8px 0 0;text-align:center;\">
                                                    <h1 style=\"color:#fff;font-family:sans-serif;margin:0;font-size:28px;\">FarmEazy Support</h1>
                                                    <p style=\"color:#e8f5e9;font-family:sans-serif;margin:6px 0 0 0;font-size:13px;letter-spacing:.3px;\">Smart Farm Care Team</p>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style=\"padding:32px 40px 24px 40px;font-family:sans-serif;color:#333;\">
                                                    <h2 style=\"color:#4CAF50;margin-top:0;\">Thank you for contacting FarmEazy Support!</h2>
                                                    <p style=\"font-size:16px;\">Your request has been received. Our team will contact you soon.</p>
                                                    <p style=\"font-size:15px;color:#555;\">If you have any further questions, simply reply to this email.</p>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style=\"background:#f4f4f7;padding:20px 40px;border-radius:0 0 8px 8px;text-align:center;color:#888;font-size:13px;\">
                                                    Thank you for using FarmEazy!<br>
                                                    <span style=\"color:#aaa;\">&copy; 2026 FarmEazy. All rights reserved.</span>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </body>
                        </html>
                """;
                emailService.sendEmail(dto.getEmail(), userSubject, userHtml, UnifiedEmailService.SenderType.SUPPORT);
    }
}

package com.farmeazy.service;

import com.farmeazy.dto.SupportTicketDto;
import com.farmeazy.dto.SupportTicketResponseDto;
import com.farmeazy.entity.Notification.NotificationPriority;
import com.farmeazy.entity.Notification.NotificationType;
import com.farmeazy.entity.SupportTicket;
import com.farmeazy.entity.SupportTicketMessage;
import com.farmeazy.entity.SupportTicket.TicketStatus;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.SupportTicketMessageRepository;
import com.farmeazy.repository.SupportTicketRepository;
import com.farmeazy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SUPPORT TICKET SERVICE
 * 
 * PURPOSE: Manages customer support tickets.
 */
@Service
public class SupportTicketService {

    private record StoredAttachment(String name, String url) {}

    private StoredAttachment storeAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
        String storedName = fileStorageService.store(file);
        return new StoredAttachment(originalName, "/uploads/" + storedName);
    }

    private List<StoredAttachment> storeAttachments(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        List<StoredAttachment> stored = new ArrayList<>();
        for (MultipartFile file : files) {
            StoredAttachment item = storeAttachment(file);
            if (item != null) {
                stored.add(item);
            }
        }
        return stored;
    }

    private String appendAttachmentsToBody(String base, List<StoredAttachment> attachments) {
        String safeBase = base != null ? base : "";
        if (attachments == null || attachments.isEmpty()) {
            return safeBase;
        }
        String lines = attachments.stream()
                .map(a -> "Attachment: " + a.name() + " (" + a.url() + ")")
                .collect(Collectors.joining("\n"));
        if (safeBase.isBlank()) {
            return lines;
        }
        return safeBase + "\n\n" + lines;
    }

    private String primaryAttachmentUrl(List<StoredAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        return attachments.get(0).url();
    }

    private static final java.util.Set<String> ALLOWED_SOURCES = java.util.Set.of(
            "support_public", "support_user", "public_app", "app_user"
    );

    private String normalizeTicketSource(String source, boolean authenticatedUser) {
        if (source != null && !source.isBlank()) {
            String normalized = source.trim().toLowerCase();
            if (ALLOWED_SOURCES.contains(normalized)) {
                return normalized;
            }
        }
        return authenticatedUser ? "support_user" : "support_public";
    }

    @Value("${farmeazy.app.support-base-url:${FARMEAZY_SUPPORT_BASE_URL:https://support.farm-eazy.com}}")
    private String supportFrontendBaseUrl;

    @Value("${farmeazy.app.public-base-url:${FARMEAZY_PUBLIC_BASE_URL:https://www.farm-eazy.com}}")
    private String publicFrontendBaseUrl;

    @Value("${farmeazy.app.base-url:${farmeazy.app.public-base-url:${FARMEAZY_PUBLIC_BASE_URL:https://www.farm-eazy.com}}}")
    private String fallbackFrontendBaseUrl;

    private String buildTicketUrl(String displayId, boolean isPublicTicket) {
        if (displayId == null || displayId.isBlank()) {
            displayId = "INC00000";
        }
        String baseUrl = supportFrontendBaseUrl;
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = fallbackFrontendBaseUrl;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://support.farm-eazy.com";
        }
        String path = isPublicTicket ? "/public/tickets/" + displayId : "/tickets/" + displayId;
        return baseUrl.replaceAll("/$", "") + path;
    }

    private String buildTicketUrl(String displayId) {
        return buildTicketUrl(displayId, false);
    }

    private String resolveDisplayId(SupportTicket ticket) {
        if (ticket == null) throw new IllegalArgumentException("Ticket cannot be null");
        if (ticket.getDisplayId() != null && !ticket.getDisplayId().isBlank()) {
            return ticket.getDisplayId();
        }
        String nextDisplayId;
        if (ticket.getId() != null) {
            nextDisplayId = String.format("INC%05d", ticket.getId());
        } else {
            nextDisplayId = "INC00000";
        }
        ticket.setDisplayId(nextDisplayId);
        ticketRepository.save(ticket);
        return nextDisplayId;
    }

    private String buildEmailTemplate(String title, String content, String ctaText, String ctaUrl) {
        String button = "";
        if (ctaText != null && ctaUrl != null) {
            button = "<p style='text-align:center;margin:24px 0;'>" +
                    "<a href='" + ctaUrl + "' style='display:inline-block;padding:12px 24px;background:#2563eb;color:#fff;border-radius:8px;text-decoration:none;font-weight:700;'>" +
                    ctaText + "</a></p>";
        }
        return "<div style='font-family: Inter, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif; background:#f4f6fb; padding:24px;'>" +
                "<div style='max-width:600px; margin:0 auto; background:#ffffff; border-radius:16px; box-shadow:0 5px 20px rgba(30, 40, 60, .08); overflow:hidden;'>" +
                "<div style='background:linear-gradient(140deg, #0b72f5 0%, #10b981 100%); padding:20px; color:#fff;'>" +
                "<h1 style='margin:0; font-size:22px; font-weight:800; letter-spacing:.5px;'>" + title + "</h1>" +
                "</div>" +
                "<div style='padding:20px; color:#1f2937; font-size:15px; line-height:1.6;'>" +
                content +
                button +
                "<hr style='margin:24px 0; border:none; border-top:1px solid #e5e7eb;'>" +
                "<p style='font-size:13px; color:#6b7280;'>If you didn’t request this, you can ignore this email. For support, contact <a href='mailto:support@farm-eazy.com'>support@farm-eazy.com</a>.</p>" +
                "</div>" +
                "</div>" +
                "</div>";
    }

    /**
     * Create support ticket for guest (no user)
     */
            @Transactional
            public SupportTicketResponseDto createGuestTicket(SupportTicketDto dto) {
                return createGuestTicketWithAttachments(dto, Collections.emptyList());
            }

            @Transactional
            public SupportTicketResponseDto createGuestTicketWithAttachment(SupportTicketDto dto, MultipartFile file) {
                List<MultipartFile> files = file != null ? List.of(file) : Collections.emptyList();
                return createGuestTicketWithAttachments(dto, files);
            }

            @Transactional
            public SupportTicketResponseDto createGuestTicketWithAttachments(SupportTicketDto dto, List<MultipartFile> files) {
                SupportTicket ticket = new SupportTicket();
                ticket.setUser(null);
                ticket.setSource(normalizeTicketSource(dto.getSource(), false));
                ticket.setRoleRequest(dto.getRoleRequest());
                ticket.setSubject(dto.getSubject());
                ticket.setDescription(dto.getDescription());
                ticket.setCategory(dto.getCategory() != null ? dto.getCategory() : SupportTicket.TicketCategory.GENERAL);
                ticket.setPriority(dto.getPriority() != null ? dto.getPriority() : SupportTicket.TicketPriority.MEDIUM);
                ticket.setStatus(SupportTicket.TicketStatus.OPEN);
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.LocalDateTime slaBy = null;
                switch (ticket.getPriority()) {
                    case URGENT:
                        slaBy = now.plusHours(24);
                        break;
                    case HIGH:
                        slaBy = now.plusHours(48);
                        break;
                    case MEDIUM:
                        slaBy = now.plusHours(36);
                        break;
                    case LOW:
                        slaBy = now.plusHours(48);
                        break;
                    default:
                        slaBy = now.plusHours(48);
                }
                ticket.setSlaBy(slaBy);
                ticket.setContactEmail(dto.getContactEmail());
                ticket.setContactPhone(dto.getContactPhone());
                ticket.setOrderId(dto.getOrderId());
                ticket.setServiceId(dto.getServiceId());
                SupportTicket saved = ticketRepository.save(ticket);
                saved.setDisplayId(String.format("INC%05d", saved.getId()));
                saved = ticketRepository.save(saved);

                List<StoredAttachment> storedAttachments = storeAttachments(files);

                // Record initial user message in conversation
                String initialBody = appendAttachmentsToBody(saved.getDescription(), storedAttachments);
                createSupportTicketMessage(saved, "USER", saved.getContactEmail(), initialBody, primaryAttachmentUrl(storedAttachments));

                logger.info("Created guest support ticket {}", saved.getDisplayId());
                String subject = "New Guest Support Ticket: " + saved.getDisplayId() + " (" + saved.getSubject() + ")";
                String html = "<h2>New Guest Support Ticket Raised</h2>" +
                    "<b>Contact Email:</b> " + saved.getContactEmail() + "<br>" +
                    "<b>Subject:</b> " + saved.getSubject() + "<br>" +
                    "<b>Description:</b><br>" + saved.getDescription() + "<br>" +
                    "<b>Category:</b> " + saved.getCategory() + "<br>" +
                    "<b>Priority:</b> " + saved.getPriority() + "<br>" +
                    "<b>Ticket ID:</b> " + saved.getDisplayId() + "<br>";

                // Notify user (confirmation email) - guest tickets should route to public ticket tracking
                String ticketUrl = buildTicketUrl(saved.getDisplayId(), true);
                String userSubject = "Your FarmEazy question has been received (" + saved.getDisplayId() + ")";
                String userHtml = "<h2>Thanks for reaching out!</h2>" +
                    "<p>Your question has been received and will be reviewed by our support team.</p>" +
                    "<b>Ticket ID:</b> " + saved.getDisplayId() + "<br>" +
                    "<b>Subject:</b> " + saved.getSubject() + "<br>" +
                    "<p>View ticket history and respond: <a href='" + ticketUrl + "'>" + ticketUrl + "</a></p>" +
                    "<p>We'll email you again once a support agent responds.</p>" +
                    "<p>If you need to update your question, please email support@farm-eazy.com</p>";
                emailService.sendEmail(saved.getContactEmail(), userSubject, userHtml, UnifiedEmailService.SenderType.SUPPORT);

                return SupportTicketResponseDto.fromEntity(saved);
            }
        @Autowired
        private SupportTicketRepository ticketRepository;
    /**
     * ADMIN: Get all tickets
     */
    @Transactional(readOnly = true)
    public List<SupportTicketResponseDto> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(SupportTicketResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * ADMIN: Get filtered and paginated tickets
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getAllTicketsFiltered(int page, int size, String status, String category, String priority, Boolean important, Boolean archived, String search) {
        java.util.List<SupportTicket> all = ticketRepository.findAllByOrderByCreatedAtDesc();

        java.util.stream.Stream<SupportTicket> stream = all.stream();

        if (status != null && !status.isBlank()) {
            try { SupportTicket.TicketStatus st = SupportTicket.TicketStatus.valueOf(status); stream = stream.filter(t -> t.getStatus() == st); } catch (Exception e) {}
        }
        if (category != null && !category.isBlank()) {
            try { SupportTicket.TicketCategory cat = SupportTicket.TicketCategory.valueOf(category); stream = stream.filter(t -> t.getCategory() == cat); } catch (Exception e) {}
        }
        if (priority != null && !priority.isBlank()) {
            try { SupportTicket.TicketPriority p = SupportTicket.TicketPriority.valueOf(priority); stream = stream.filter(t -> t.getPriority() == p); } catch (Exception e) {}
        }
        if (important != null) {
            stream = stream.filter(t -> Boolean.TRUE.equals(t.getImportant()) == important);
        }
        if (archived != null) {
            stream = stream.filter(t -> Boolean.TRUE.equals(t.getArchived()) == archived);
        }
        if (search != null && !search.isBlank()) {
            String s = search.toLowerCase();
            stream = stream.filter(t -> (t.getDisplayId() != null && t.getDisplayId().toLowerCase().contains(s)) || (t.getSubject() != null && t.getSubject().toLowerCase().contains(s)) || (t.getDescription() != null && t.getDescription().toLowerCase().contains(s)));
        }

        java.util.List<SupportTicket> filtered = stream.collect(Collectors.toList());
        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        java.util.List<SupportTicketResponseDto> pageList = filtered.subList(from, to).stream().map(SupportTicketResponseDto::fromEntity).collect(Collectors.toList());

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("tickets", pageList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    private SupportTicket resolveTicket(String displayId) {
        return ticketRepository.findByDisplayId(displayId)
                .or(() -> {
                    try {
                        long id = Long.parseLong(displayId);
                        return ticketRepository.findById(id);
                    } catch (NumberFormatException ex) {
                        return Optional.empty();
                    }
                })
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
    }

    @Transactional
    public SupportTicketResponseDto setImportant(String displayId, boolean important) {
        SupportTicket ticket = resolveTicket(displayId);
        ticket.setImportant(important);
        ticketRepository.save(ticket);

        createSupportTicketMessage(ticket, "SYSTEM", "System", "Important flag set to " + important + "", null);

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional
    public SupportTicketResponseDto setArchived(String displayId, boolean archived) {
        SupportTicket ticket = resolveTicket(displayId);
        ticket.setArchived(archived);
        ticketRepository.save(ticket);

        createSupportTicketMessage(ticket, "SYSTEM", "System", "Archived flag set to " + archived + "", null);

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional
    public SupportTicketResponseDto setSla(String displayId, LocalDateTime slaBy) {
        SupportTicket ticket = resolveTicket(displayId);
        ticket.setSlaBy(slaBy);
        ticketRepository.save(ticket);
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    private void createSupportTicketMessage(SupportTicket ticket, String senderType, String senderName, String message, String attachmentUrl) {
        SupportTicketMessage event = new SupportTicketMessage(ticket.getId(), senderType, senderName, message, attachmentUrl);
        supportTicketMessageRepository.save(event);
    }

    /**
     * ADMIN: Set ticket status (ADMIN or SUPERADMIN)
     */
    @Transactional
    public SupportTicketResponseDto setStatusAdmin(String displayId, String statusStr) {
        SupportTicket ticket = resolveTicket(displayId);
        try {
            SupportTicket.TicketStatus st = SupportTicket.TicketStatus.valueOf(statusStr);
            SupportTicket.TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(st);
            if (st == SupportTicket.TicketStatus.RESOLVED) ticket.setResolvedAt(LocalDateTime.now());
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            String statusChangeMessage = "Status changed from " + oldStatus + " to " + st + ".";
            createSupportTicketMessage(ticket, "SYSTEM", "System", statusChangeMessage, null);

                String resolvedDisplayId = resolveDisplayId(ticket);
                notifyTicketOwner(
                    ticket,
                    "Ticket " + resolvedDisplayId + " status updated",
                    statusChangeMessage,
                    "/user/tickets/" + resolvedDisplayId,
                    (st == TicketStatus.RESOLVED || st == TicketStatus.CLOSED) ? NotificationPriority.HIGH : NotificationPriority.NORMAL
                );

            return SupportTicketResponseDto.fromEntity(ticket);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ticket status: " + statusStr);
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<com.farmeazy.dto.SupportTicketMessageDto> getTicketMessages(String displayId) {
        SupportTicket ticket = resolveTicket(displayId);
        java.util.List<com.farmeazy.dto.SupportTicketMessageDto> messages = supportTicketMessageRepository
                .findBySupportTicketIdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(com.farmeazy.dto.SupportTicketMessageDto::fromEntity)
                .toList();

        // Fallback for legacy tickets with no message rows
        if (messages.isEmpty()) {
            if (ticket.getDescription() != null) {
                com.farmeazy.dto.SupportTicketMessageDto userInitial = new com.farmeazy.dto.SupportTicketMessageDto();
                userInitial.setId(-1L);
                userInitial.setSenderType("USER");
                userInitial.setSenderName(ticket.getContactEmail());
                userInitial.setMessage(ticket.getDescription());
                userInitial.setCreatedAt(ticket.getCreatedAt());
                messages.add(userInitial);
            }
            if (ticket.getAdminNotes() != null && !ticket.getAdminNotes().isBlank()) {
                com.farmeazy.dto.SupportTicketMessageDto adminLegacy = new com.farmeazy.dto.SupportTicketMessageDto();
                adminLegacy.setId(-1L);
                adminLegacy.setSenderType("ADMIN");
                adminLegacy.setSenderName("Admin");
                adminLegacy.setMessage(ticket.getAdminNotes());
                adminLegacy.setCreatedAt(ticket.getUpdatedAt());
                messages.add(adminLegacy);
            }
        }

        return messages;
    }

    /**
     * ADMIN: Reply to any ticket
     */
    @Transactional
    public SupportTicketResponseDto adminReplyToTicket(String adminEmail, String displayId, String reply) {
        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("Reply cannot be empty");
        }
        if (reply.length() > 4000) {
            throw new IllegalArgumentException("Reply is too long");
        }

        SupportTicket ticket = resolveTicket(displayId);
        // Maintain legacy admin notes for backwards compatibility
        String notes = (ticket.getAdminNotes() != null ? ticket.getAdminNotes() : "") +
                "\n\n--- Admin Reply (" + LocalDateTime.now() + ", " + adminEmail + ") ---\n" + reply;
        ticket.setAdminNotes(notes);

        // Move status to IN_PROGRESS if not already
        if (ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.PENDING_USER) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // Add new conversation record
        createSupportTicketMessage(ticket, "ADMIN", adminEmail, reply, null);

        logger.info("Admin {} replied to ticket {}", adminEmail, displayId);

        // Notify user
        String userEmail = ticket.getContactEmail();
        String resolvedDisplayId = resolveDisplayId(ticket);
        boolean isPublicTicket = ticket.getUser() == null;
        String ticketUrl = buildTicketUrl(resolvedDisplayId, isPublicTicket);
        String subject = "Support Ticket Update: " + resolvedDisplayId;
        String html = "<h2>Your support ticket has been updated by admin</h2>" +
                "<p><strong>Ticket:</strong> " + resolvedDisplayId + "</p>" +
                "<b>Subject:</b> " + ticket.getSubject() + "<br>" +
                "<b>Admin Reply:</b><br>" + reply + "<br>" +
                "<b>Status:</b> " + ticket.getStatus() + "<br>" +
                "<p>View/update your ticket: <a href='" + ticketUrl + "'>" + ticketUrl + "</a></p>";
        emailService.sendEmail(userEmail, subject, html, UnifiedEmailService.SenderType.SUPPORT);

        notifyTicketOwner(
            ticket,
            "New support reply on " + resolvedDisplayId,
            "Support has replied to your ticket.",
            "/user/tickets/" + resolvedDisplayId,
            NotificationPriority.HIGH
        );
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * ADMIN: Resolve ticket
     */
    @Transactional
    public SupportTicketResponseDto resolveTicketAdmin(String adminEmail, String displayId, String resolution) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        ticket.setResolution(resolution);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        createSupportTicketMessage(ticket, "SYSTEM", "System", "Ticket marked RESOLVED by " + adminEmail, null);

        logger.info("Admin {} resolved ticket {}", adminEmail, displayId);
            // Trigger email notification to user
            String userEmail = ticket.getContactEmail();
            String resolvedDisplayId = resolveDisplayId(ticket);
            boolean isPublicTicket = ticket.getUser() == null;
            String ticketUrl = buildTicketUrl(resolvedDisplayId, isPublicTicket);
            String subject = "Support Ticket Resolved: " + resolvedDisplayId;
            String html = "<h2>Your support ticket has been resolved</h2>" +
                "<p><strong>Ticket:</strong> " + resolvedDisplayId + "</p>" +
                "<b>Ticket:</b> " + ticket.getDisplayId() + "<br>" +
                "<b>Subject:</b> " + ticket.getSubject() + "<br>" +
                "<b>Resolution:</b><br>" + resolution + "<br>" +
                "<b>Status:</b> " + ticket.getStatus() + "<br>" +
                "<p>View ticket history & reopen if needed: <a href='" + ticketUrl + "'>" + ticketUrl + "</a></p>";
            emailService.sendEmail(userEmail, subject, html, UnifiedEmailService.SenderType.SUPPORT);

        notifyTicketOwner(
                ticket,
                "Ticket " + resolvedDisplayId + " resolved",
                "Your ticket has been resolved by support.",
                "/user/tickets/" + resolvedDisplayId,
                NotificationPriority.HIGH
        );
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketService.class);


    @Autowired
    private SupportTicketMessageRepository supportTicketMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UnifiedEmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FileStorageService fileStorageService;

    private void notifyTicketOwner(SupportTicket ticket, String title, String message, String actionUrl, NotificationPriority priority) {
        if (ticket == null || ticket.getUser() == null) {
            return;
        }
        notificationService.createForUser(ticket.getUser(), NotificationType.SYSTEM, title, message, actionUrl, priority);
    }

    /**
     * ADMIN: Upload attachment for a support ticket and append link to admin notes
     */
    @Transactional
    public SupportTicketResponseDto adminUploadAttachment(String displayId, org.springframework.web.multipart.MultipartFile file, String adminEmail) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        try {
            StoredAttachment storedAttachment = storeAttachment(file);
            String fileUrl = storedAttachment != null ? storedAttachment.url() : null;
            String fileName = storedAttachment != null ? storedAttachment.name() : (file != null ? file.getOriginalFilename() : "attachment");
            String notes = (ticket.getAdminNotes() != null ? ticket.getAdminNotes() : "") +
                    "\n\n--- Admin Attachment (" + java.time.LocalDateTime.now() + ", " + adminEmail + ") ---\n" +
                "Attachment: " + fileName + " (" + fileUrl + ")\n";
            ticket.setAdminNotes(notes);
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            createSupportTicketMessage(ticket, "ADMIN", adminEmail, "Attached file: " + fileName, fileUrl);

            logger.info("Admin {} uploaded attachment to ticket {}", adminEmail, displayId);
            // Optionally notify user
            String userEmail = ticket.getContactEmail();
            String subject = "Support Ticket Updated with Attachment: " + ticket.getDisplayId();
            String html = "<h2>An attachment was added to your support ticket</h2>" +
                    "<b>Ticket:</b> " + ticket.getDisplayId() + "<br>" +
                    "<b>Attachment:</b> " + fileName + "<br>";
            emailService.sendEmail(userEmail, subject, html, UnifiedEmailService.SenderType.SUPPORT);
            return SupportTicketResponseDto.fromEntity(ticket);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload attachment", ex);
        }
    }

    /**
     * ADMIN: Reply with optional attachment in a single operation
     */
    @Transactional
    public SupportTicketResponseDto adminReplyWithAttachment(String adminEmail, String displayId, String reply, org.springframework.web.multipart.MultipartFile file) {
        List<MultipartFile> files = file != null && !file.isEmpty() ? List.of(file) : Collections.emptyList();
        return adminReplyWithAttachments(adminEmail, displayId, reply, files);
    }

    @Transactional
    public SupportTicketResponseDto adminReplyWithAttachments(String adminEmail, String displayId, String reply, List<MultipartFile> files) {
        boolean hasReply = reply != null && !reply.isBlank();
        List<StoredAttachment> storedAttachments = storeAttachments(files);
        boolean hasFile = !storedAttachments.isEmpty();
        if (!hasReply && !hasFile) {
            throw new IllegalArgumentException("Provide a reply message or attachment");
        }
        if (hasReply && reply.length() > 4000) {
            throw new IllegalArgumentException("Reply is too long");
        }

        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));

        String replyText = reply != null ? reply : "";
        String responseMessage = appendAttachmentsToBody(replyText, storedAttachments);

        // Append admin reply and optional attachment to adminNotes
        String notes = (ticket.getAdminNotes() != null ? ticket.getAdminNotes() : "") +
                "\n\n--- Admin Reply (" + LocalDateTime.now() + ", " + adminEmail + ") ---\n" + responseMessage;
        ticket.setAdminNotes(notes);

        // Move status to IN_PROGRESS if appropriate
        if (ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.PENDING_USER) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // Add message row
        if (hasReply) {
            createSupportTicketMessage(ticket, "ADMIN", adminEmail, responseMessage, primaryAttachmentUrl(storedAttachments));
        } else {
            createSupportTicketMessage(ticket, "ADMIN", adminEmail, responseMessage, primaryAttachmentUrl(storedAttachments));
        }

        // Notify user once about reply and/or attachment
        try {
            String userEmail = ticket.getContactEmail();
            boolean isPublicTicket = ticket.getUser() == null;
            String ticketUrl = buildTicketUrl(ticket.getDisplayId(), isPublicTicket);
            String subject = "Support Ticket Update: " + ticket.getDisplayId();
            StringBuilder html = new StringBuilder();
            html.append("<h2>Your support ticket has been updated by admin</h2>");
            html.append("<b>Ticket:</b> " + ticket.getDisplayId() + "<br>");
            if (hasReply) html.append("<b>Admin Reply:</b><br>" + reply + "<br>");
            if (hasFile) {
                String fileNames = storedAttachments.stream().map(StoredAttachment::name).collect(Collectors.joining(", "));
                html.append("<b>Attachment(s):</b> " + fileNames + "<br>");
            }
            html.append("<b>Status:</b> " + ticket.getStatus() + "<br>");
            html.append("<p>View ticket: <a href='" + ticketUrl + "'>" + ticketUrl + "</a></p>");
            emailService.sendEmail(userEmail, subject, html.toString(), UnifiedEmailService.SenderType.SUPPORT);

            notifyTicketOwner(
                    ticket,
                    "Ticket " + ticket.getDisplayId() + " updated",
                        "Support posted an update" + (hasFile ? " with attachment." : "."),
                    "/user/tickets/" + ticket.getDisplayId(),
                    NotificationPriority.HIGH
            );
        } catch (Exception ex) {
            logger.warn("Failed to send notification after admin reply/attachment", ex);
        }

        logger.info("Admin {} replied to ticket {} with attachments={}", adminEmail, displayId, storedAttachments.size());
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * Create a new support ticket
     */
    @Transactional
    public SupportTicketResponseDto createTicket(String userEmail, SupportTicketDto dto) {
        return createTicketWithAttachments(userEmail, dto, Collections.emptyList());
    }

    @Transactional
    public SupportTicketResponseDto createTicketWithAttachment(String userEmail, SupportTicketDto dto, MultipartFile file) {
        List<MultipartFile> files = file != null ? List.of(file) : Collections.emptyList();
        return createTicketWithAttachments(userEmail, dto, files);
    }

    @Transactional
    public SupportTicketResponseDto createTicketWithAttachments(String userEmail, SupportTicketDto dto, List<MultipartFile> files) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setSource(normalizeTicketSource(dto.getSource(), true));
        ticket.setRoleRequest(dto.getRoleRequest());
        ticket.setSubject(dto.getSubject());
        ticket.setDescription(dto.getDescription());
        ticket.setCategory(dto.getCategory() != null ? dto.getCategory() : SupportTicket.TicketCategory.GENERAL);
        ticket.setPriority(dto.getPriority() != null ? dto.getPriority() : SupportTicket.TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        // Set SLA by default based on priority
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime slaBy = null;
        switch (ticket.getPriority()) {
            case URGENT:
                slaBy = now.plusHours(24);
                break;
            case HIGH:
                slaBy = now.plusHours(48);
                break;
            case MEDIUM:
                slaBy = now.plusHours(36);
                break;
            case LOW:
                slaBy = now.plusHours(48);
                break;
            default:
                slaBy = now.plusHours(48);
        }
        ticket.setSlaBy(slaBy);
        ticket.setContactEmail(dto.getContactEmail() != null ? dto.getContactEmail() : user.getEmail());
        ticket.setContactPhone(dto.getContactPhone() != null ? dto.getContactPhone() : user.getPhone());
        ticket.setOrderId(dto.getOrderId());
        ticket.setServiceId(dto.getServiceId());

        // Save to get ID
        SupportTicket saved = ticketRepository.save(ticket);
        
        // Generate display ID
        saved.setDisplayId(String.format("INC%05d", saved.getId()));
        saved = ticketRepository.save(saved);

        List<StoredAttachment> storedAttachments = storeAttachments(files);

        // Record initial user message in conversation
        String initialBody = appendAttachmentsToBody(saved.getDescription(), storedAttachments);
        createSupportTicketMessage(saved, "USER", saved.getContactEmail(), initialBody, primaryAttachmentUrl(storedAttachments));

        logger.info("Created support ticket {} for user {}", saved.getDisplayId(), userEmail);
        // Send email to support@farm-eazy.com
        String subject = "New Support Ticket: " + saved.getDisplayId() + " (" + saved.getSubject() + ")";
        String html = "<h2>New Support Ticket Raised</h2>" +
            "<b>User:</b> " + user.getEmail() + "<br>" +
            "<b>Subject:</b> " + saved.getSubject() + "<br>" +
            "<b>Description:</b><br>" + saved.getDescription() + "<br>" +
            "<b>Category:</b> " + saved.getCategory() + "<br>" +
            "<b>Priority:</b> " + saved.getPriority() + "<br>" +
            "<b>Ticket ID:</b> " + saved.getDisplayId() + "<br>";
        emailService.sendEmail("support@farm-eazy.com", subject, html, UnifiedEmailService.SenderType.SUPPORT);

        String resolvedDisplayId = resolveDisplayId(saved);
        String userTicketUrl = buildTicketUrl(resolvedDisplayId, false);
        String userSubject = "Your FarmEazy support ticket has been created (" + resolvedDisplayId + ")";
        String userHtml = buildEmailTemplate(
            "Your support ticket is now live",
            "<p>Hi " + (user.getUsername() != null ? user.getUsername() : user.getEmail()) + ",</p>" +
            "<p>Your ticket has been successfully created. Our support team is reviewing it and will respond shortly.</p>" +
            "<ul style='padding-left:20px; margin: 0;'><li><strong>Ticket ID:</strong> " + saved.getDisplayId() + "</li>" +
            "<li><strong>Subject:</strong> " + saved.getSubject() + "</li>" +
            "<li><strong>Priority:</strong> " + saved.getPriority() + "</li></ul>",
            "Open your ticket and reply",
            userTicketUrl
        );
        emailService.sendEmail(saved.getContactEmail(), userSubject, userHtml, UnifiedEmailService.SenderType.SUPPORT);

            notifyTicketOwner(
                saved,
                "Ticket " + resolvedDisplayId + " submitted",
                "Your support request has been created successfully.",
                "/user/tickets/" + resolvedDisplayId,
                NotificationPriority.NORMAL
            );

        return SupportTicketResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public SupportTicketResponseDto getPublicTicketByDisplayId(String displayId) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional
    public SupportTicketResponseDto addPublicResponse(String displayId, String response, String senderEmail) {
        return addPublicResponseWithAttachments(displayId, response, senderEmail, Collections.emptyList());
    }

    @Transactional
    public SupportTicketResponseDto addPublicResponseWithAttachment(String displayId, String response, String senderEmail, MultipartFile file) {
        List<MultipartFile> files = file != null ? List.of(file) : Collections.emptyList();
        return addPublicResponseWithAttachments(displayId, response, senderEmail, files);
    }

    @Transactional
    public SupportTicketResponseDto addPublicResponseWithAttachments(String displayId, String response, String senderEmail, List<MultipartFile> files) {
        boolean hasResponse = response != null && !response.isBlank();
        List<StoredAttachment> storedAttachments = storeAttachments(files);
        if (!hasResponse && storedAttachments.isEmpty()) {
            throw new IllegalArgumentException("Response message or attachment is required");
        }

        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));

        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot respond to a closed/cancelled ticket");
        }

        String sender = senderEmail != null ? senderEmail : ticket.getContactEmail();
        String messageBody = hasResponse ? response : "Attachment added";
        messageBody = appendAttachmentsToBody(messageBody, storedAttachments);
        createSupportTicketMessage(ticket, "USER", sender, messageBody, primaryAttachmentUrl(storedAttachments));

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // Notify support team about user reply
        String supportSubject = "Public ticket response received: " + ticket.getDisplayId();
        String userLink = buildTicketUrl(ticket.getDisplayId(), true);
        String supportHtml = "<h2>User replied to ticket</h2>" +
                "<b>Ticket:</b> " + ticket.getDisplayId() + "<br>" +
                "<b>From:</b> " + sender + "<br>" +
                "<b>Response:</b><br>" + messageBody + "<br>" +
                "<b>View ticket:</b> <a href='" + userLink + "'>" + userLink + "</a><br>";
        emailService.sendEmail("support@farm-eazy.com", supportSubject, supportHtml, UnifiedEmailService.SenderType.SUPPORT);

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional
    public SupportTicketResponseDto reopenPublicTicket(String displayId, String requesterEmail) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reopen a cancelled ticket");
        }

        ticket.setStatus(TicketStatus.OPEN);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        createSupportTicketMessage(ticket, "SYSTEM", "System", "Ticket reopened by " + (requesterEmail != null ? requesterEmail : "public user"), null);

        String userLink = buildTicketUrl(ticket.getDisplayId(), true);
        String userSubject = "Your support ticket is reopened: " + ticket.getDisplayId();
        String userHtml = "<h2>Your ticket has been reopened</h2>" +
                "<b>Ticket:</b> " + ticket.getDisplayId() + "<br>" +
                "<b>Subject:</b> " + ticket.getSubject() + "<br>" +
                "<b>View:</b> <a href='" + userLink + "'>" + userLink + "</a><br>";
        emailService.sendEmail(ticket.getContactEmail(), userSubject, userHtml, UnifiedEmailService.SenderType.SUPPORT);

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * Get all tickets for a user
     */
    @Transactional(readOnly = true)
    public List<SupportTicketResponseDto> getUserTickets(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ticketRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(SupportTicketResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific ticket by display ID
     */
    @Transactional(readOnly = true)
    public SupportTicketResponseDto getTicketByDisplayId(String userEmail, String displayId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));

        // Verify ownership
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Ticket not found: " + displayId);
        }

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * ADMIN: Get a ticket by display ID without ownership checks
     */
    @Transactional(readOnly = true)
    public SupportTicketResponseDto getTicketByDisplayIdAdmin(String displayId) {
        Optional<SupportTicket> ticketOpt = ticketRepository.findByDisplayId(displayId);
        if (ticketOpt.isEmpty()) {
            try {
                long id = Long.parseLong(displayId);
                ticketOpt = ticketRepository.findById(id);
            } catch (NumberFormatException ignored) {
                // Not numeric ID, ignore fallback
            }
        }
        SupportTicket ticket = ticketOpt.orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * Cancel a ticket (user action)
     */
    @Transactional
    public SupportTicketResponseDto cancelTicket(String userEmail, String displayId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));

        // Verify ownership
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Ticket not found: " + displayId);
        }

        // Can only cancel OPEN or IN_PROGRESS tickets
        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot cancel ticket with status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setClosedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        logger.info("Ticket {} cancelled by user {}", displayId, userEmail);

        notifyTicketOwner(
            ticket,
            "Ticket " + displayId + " cancelled",
            "Your ticket was marked as cancelled.",
            "/user/tickets/" + displayId,
            NotificationPriority.NORMAL
        );

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * Add response to a ticket (updates description)
     */
    @Transactional
    public SupportTicketResponseDto addResponse(String userEmail, String displayId, String response) {
        return addResponseWithAttachments(userEmail, displayId, response, Collections.emptyList());
    }

    @Transactional
    public SupportTicketResponseDto addResponseWithAttachment(String userEmail, String displayId, String response, MultipartFile file) {
        List<MultipartFile> files = file != null ? List.of(file) : Collections.emptyList();
        return addResponseWithAttachments(userEmail, displayId, response, files);
    }

    @Transactional
    public SupportTicketResponseDto addResponseWithAttachments(String userEmail, String displayId, String response, List<MultipartFile> files) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));

        // Verify ownership
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Ticket not found: " + displayId);
        }

        // Can only add response to open tickets
        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot add response to closed ticket");
        }

        boolean hasResponse = response != null && !response.isBlank();
        List<StoredAttachment> storedAttachments = storeAttachments(files);
        if (!hasResponse && storedAttachments.isEmpty()) {
            throw new IllegalArgumentException("Response message or attachment is required");
        }

        // Append response to description
        String responseText = hasResponse ? response : "Attachment added";
        String updatedDescription = ticket.getDescription() + "\n\n--- User Response (" + LocalDateTime.now() + ") ---\n" + responseText;
        String responseMessage = appendAttachmentsToBody(responseText, storedAttachments);
        if (!storedAttachments.isEmpty()) {
            String attachmentLines = storedAttachments.stream()
                    .map(a -> "Attachment: " + a.name() + " (" + a.url() + ")")
                    .collect(Collectors.joining("\n"));
            updatedDescription += "\n" + attachmentLines;
        }
        ticket.setDescription(updatedDescription);

        // Persist the response as a conversation message so both user/admin history stays in sync.
        createSupportTicketMessage(ticket, "USER", userEmail, responseMessage, primaryAttachmentUrl(storedAttachments));
        
        // User has replied, ticket should return to active support handling.
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        logger.info("Response added to ticket {} by user {}", displayId, userEmail);

        notifyTicketOwner(
            ticket,
            "Reply sent for " + displayId,
            "Your response was added to the ticket conversation.",
            "/user/tickets/" + displayId,
            NotificationPriority.NORMAL
        );

        try {
            String supportSubject = "User replied on ticket: " + resolveDisplayId(ticket);
            String supportHtml = "<h2>User replied on support ticket</h2>" +
                    "<b>Ticket:</b> " + resolveDisplayId(ticket) + "<br>" +
                    "<b>User:</b> " + userEmail + "<br>" +
                    "<b>Message:</b><br>" + responseText + "<br>";
            emailService.sendEmail("support@farm-eazy.com", supportSubject, supportHtml, UnifiedEmailService.SenderType.SUPPORT);
        } catch (Exception ex) {
            logger.warn("Failed sending support email notification for ticket user reply {}", displayId, ex);
        }

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * Get count of active tickets for a user
     */
    @Transactional(readOnly = true)
    public long getActiveTicketCount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ticketRepository.countByUserAndStatus(user, TicketStatus.OPEN) +
               ticketRepository.countByUserAndStatus(user, TicketStatus.IN_PROGRESS) +
               ticketRepository.countByUserAndStatus(user, TicketStatus.PENDING_USER);
    }
}

package com.farmeazy.service;

import com.farmeazy.dto.SupportTicketDto;
import com.farmeazy.dto.SupportTicketResponseDto;
import com.farmeazy.entity.SupportTicket;
import com.farmeazy.entity.SupportTicket.TicketStatus;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.SupportTicketRepository;
import com.farmeazy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SUPPORT TICKET SERVICE
 * 
 * PURPOSE: Manages customer support tickets.
 */
@Service
public class SupportTicketService {
            /**
             * Create support ticket for guest (no user)
             */
            @Transactional
            public SupportTicketResponseDto createGuestTicket(SupportTicketDto dto) {
                SupportTicket ticket = new SupportTicket();
                ticket.setUser(null);
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
                logger.info("Created guest support ticket {}", saved.getDisplayId());
                String subject = "New Guest Support Ticket: " + saved.getDisplayId() + " (" + saved.getSubject() + ")";
                String html = "<h2>New Guest Support Ticket Raised</h2>" +
                    "<b>Contact Email:</b> " + saved.getContactEmail() + "<br>" +
                    "<b>Subject:</b> " + saved.getSubject() + "<br>" +
                    "<b>Description:</b><br>" + saved.getDescription() + "<br>" +
                    "<b>Category:</b> " + saved.getCategory() + "<br>" +
                    "<b>Priority:</b> " + saved.getPriority() + "<br>" +
                    "<b>Ticket ID:</b> " + saved.getDisplayId() + "<br>";
                emailService.sendEmail("support@farm-eazy.com", subject, html, UnifiedEmailService.SenderType.SUPPORT);
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

    @Transactional
    public SupportTicketResponseDto setImportant(String displayId, boolean important) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId).orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        ticket.setImportant(important);
        ticketRepository.save(ticket);
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional
    public SupportTicketResponseDto setArchived(String displayId, boolean archived) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId).orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        ticket.setArchived(archived);
        ticketRepository.save(ticket);
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional
    public SupportTicketResponseDto setSla(String displayId, LocalDateTime slaBy) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId).orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        ticket.setSlaBy(slaBy);
        ticketRepository.save(ticket);
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * ADMIN: Set ticket status (ADMIN or SUPERADMIN)
     */
    @Transactional
    public SupportTicketResponseDto setStatusAdmin(String displayId, String statusStr) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId).orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        try {
            SupportTicket.TicketStatus st = SupportTicket.TicketStatus.valueOf(statusStr);
            ticket.setStatus(st);
            if (st == SupportTicket.TicketStatus.RESOLVED) ticket.setResolvedAt(LocalDateTime.now());
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketRepository.save(ticket);
            return SupportTicketResponseDto.fromEntity(ticket);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ticket status: " + statusStr);
        }
    }

    /**
     * ADMIN: Reply to any ticket
     */
    @Transactional
    public SupportTicketResponseDto adminReplyToTicket(String adminEmail, String displayId, String reply) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        // Append admin reply to adminNotes
        String notes = (ticket.getAdminNotes() != null ? ticket.getAdminNotes() : "") +
                "\n\n--- Admin Reply (" + LocalDateTime.now() + ", " + adminEmail + ") ---\n" + reply;
        ticket.setAdminNotes(notes);
        // Move status to IN_PROGRESS if not already
        if (ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.PENDING_USER) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);
        logger.info("Admin {} replied to ticket {}", adminEmail, displayId);
            // Trigger email notification to user
            String userEmail = ticket.getContactEmail();
            String subject = "Support Ticket Update: " + ticket.getDisplayId();
            String html = "<h2>Your support ticket has been updated by admin</h2>" +
                "<b>Ticket:</b> " + ticket.getDisplayId() + "<br>" +
                "<b>Subject:</b> " + ticket.getSubject() + "<br>" +
                "<b>Admin Reply:</b><br>" + reply + "<br>" +
                "<b>Status:</b> " + ticket.getStatus() + "<br>" +
                "<b>View your ticket in the dashboard.</b>";
            emailService.sendEmail(userEmail, subject, html, UnifiedEmailService.SenderType.SUPPORT);
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
        logger.info("Admin {} resolved ticket {}", adminEmail, displayId);
            // Trigger email notification to user
            String userEmail = ticket.getContactEmail();
            String subject = "Support Ticket Resolved: " + ticket.getDisplayId();
            String html = "<h2>Your support ticket has been resolved</h2>" +
                "<b>Ticket:</b> " + ticket.getDisplayId() + "<br>" +
                "<b>Subject:</b> " + ticket.getSubject() + "<br>" +
                "<b>Resolution:</b><br>" + resolution + "<br>" +
                "<b>Status:</b> " + ticket.getStatus() + "<br>" +
                "<b>View your ticket in the dashboard.</b>";
            emailService.sendEmail(userEmail, subject, html, UnifiedEmailService.SenderType.SUPPORT);
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketService.class);


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UnifiedEmailService emailService;

    /**
     * ADMIN: Upload attachment for a support ticket and append link to admin notes
     */
    @Transactional
    public SupportTicketResponseDto adminUploadAttachment(String displayId, org.springframework.web.multipart.MultipartFile file, String adminEmail) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        try {
            java.nio.file.Path uploads = java.nio.file.Paths.get("uploads");
            if (!java.nio.file.Files.exists(uploads)) java.nio.file.Files.createDirectories(uploads);
            String filename = System.currentTimeMillis() + "-" + java.util.UUID.randomUUID() + "-" + file.getOriginalFilename();
            java.nio.file.Path target = uploads.resolve(filename);
            try (java.io.InputStream in = file.getInputStream()) {
                java.nio.file.Files.copy(in, target);
            }
            String fileUrl = "/uploads/" + filename;
            String notes = (ticket.getAdminNotes() != null ? ticket.getAdminNotes() : "") +
                    "\n\n--- Admin Attachment (" + java.time.LocalDateTime.now() + ", " + adminEmail + ") ---\n" +
                    "Attachment: " + file.getOriginalFilename() + " (" + fileUrl + ")\n";
            ticket.setAdminNotes(notes);
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketRepository.save(ticket);
            logger.info("Admin {} uploaded attachment to ticket {}", adminEmail, displayId);
            // Optionally notify user
            String userEmail = ticket.getContactEmail();
            String subject = "Support Ticket Updated with Attachment: " + ticket.getDisplayId();
            String html = "<h2>An attachment was added to your support ticket</h2>" +
                    "<b>Ticket:</b> " + ticket.getDisplayId() + "<br>" +
                    "<b>Attachment:</b> " + file.getOriginalFilename() + "<br>";
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
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));

        // Save attachment if provided
        String attachmentLine = null;
        if (file != null && !file.isEmpty()) {
            try {
                java.nio.file.Path uploads = java.nio.file.Paths.get("uploads");
                if (!java.nio.file.Files.exists(uploads)) java.nio.file.Files.createDirectories(uploads);
                String filename = System.currentTimeMillis() + "-" + java.util.UUID.randomUUID() + "-" + file.getOriginalFilename();
                java.nio.file.Path target = uploads.resolve(filename);
                try (java.io.InputStream in = file.getInputStream()) {
                    java.nio.file.Files.copy(in, target);
                }
                String fileUrl = "/uploads/" + filename;
                attachmentLine = "\nAttachment: " + file.getOriginalFilename() + " (" + fileUrl + ")\n";
            } catch (Exception ex) {
                throw new RuntimeException("Failed to save attachment", ex);
            }
        }

        // Append admin reply and optional attachment to adminNotes
        String notes = (ticket.getAdminNotes() != null ? ticket.getAdminNotes() : "") +
                "\n\n--- Admin Reply (" + LocalDateTime.now() + ", " + adminEmail + ") ---\n" + (reply != null ? reply : "") +
                (attachmentLine != null ? attachmentLine : "");
        ticket.setAdminNotes(notes);

        // Move status to IN_PROGRESS if appropriate
        if (ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.PENDING_USER) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // Notify user once about reply and/or attachment
        try {
            String userEmail = ticket.getContactEmail();
            String subject = "Support Ticket Update: " + ticket.getDisplayId();
            StringBuilder html = new StringBuilder();
            html.append("<h2>Your support ticket has been updated by admin</h2>");
            html.append("<b>Ticket:</b> " + ticket.getDisplayId() + "<br>");
            if (reply != null && !reply.isBlank()) html.append("<b>Admin Reply:</b><br>" + reply + "<br>");
            if (attachmentLine != null) html.append("<b>Attachment:</b> " + file.getOriginalFilename() + "<br>");
            html.append("<b>Status:</b> " + ticket.getStatus() + "<br>");
            emailService.sendEmail(userEmail, subject, html.toString(), UnifiedEmailService.SenderType.SUPPORT);
        } catch (Exception ex) {
            logger.warn("Failed to send notification after admin reply/attachment", ex);
        }

        logger.info("Admin {} replied to ticket {} with attachment={}", adminEmail, displayId, (file != null && !file.isEmpty()));
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * Create a new support ticket
     */
    @Transactional
    public SupportTicketResponseDto createTicket(String userEmail, SupportTicketDto dto) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
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

        return SupportTicketResponseDto.fromEntity(saved);
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
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
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

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * Add response to a ticket (updates description)
     */
    @Transactional
    public SupportTicketResponseDto addResponse(String userEmail, String displayId, String response) {
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

        // Append response to description
        String updatedDescription = ticket.getDescription() + "\n\n--- User Response (" + LocalDateTime.now() + ") ---\n" + response;
        ticket.setDescription(updatedDescription);
        
        // If pending user response, move back to open
        if (ticket.getStatus() == TicketStatus.PENDING_USER) {
            ticket.setStatus(TicketStatus.OPEN);
        }
        
        ticketRepository.save(ticket);

        logger.info("Response added to ticket {} by user {}", displayId, userEmail);

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

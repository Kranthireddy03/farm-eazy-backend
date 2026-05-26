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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SUPPORT TICKET SERVICE
 * 
 * PURPOSE: Manages customer support tickets.
 */
@Service
public class SupportTicketService {

    private static final List<TicketStatus> ACTIVE_TICKET_STATUSES = List.of(
            TicketStatus.OPEN,
            TicketStatus.IN_PROGRESS,
            TicketStatus.PENDING_USER
    );

    private record StoredAttachment(String name, String url) {}

        private static final int MAX_ATTACHMENTS = 3;
        private static final long MAX_ATTACHMENT_SIZE = 5L * 1024L * 1024L; // 5 MB
        private static final java.util.Set<String> ALLOWED_CONTENT_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/jpg", "image/gif", "image/webp",
            "application/pdf", "text/plain", "text/csv",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

    private StoredAttachment storeAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new IllegalArgumentException("Attachment exceeds maximum size of 5 MB: " + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"));
        }
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Attachment has disallowed content type: " + contentType);
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
        String storedName = fileStorageService.store(file);
        return new StoredAttachment(originalName, "/uploads/" + storedName);
    }

    private List<StoredAttachment> storeAttachments(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        if (files.size() > MAX_ATTACHMENTS) {
            throw new IllegalArgumentException("Maximum " + MAX_ATTACHMENTS + " attachments allowed");
        }
        java.util.Map<String, StoredAttachment> unique = new java.util.LinkedHashMap<>();
        for (MultipartFile file : files) {
            StoredAttachment item = storeAttachment(file);
            if (item != null) {
                unique.putIfAbsent(item.url(), item);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private String appendAttachmentsToBody(String base, List<StoredAttachment> attachments) {
        String safeBase = base != null ? base : "";
        if (attachments == null || attachments.isEmpty()) {
            return safeBase;
        }
        String lines = attachments.stream()
                .map(a -> "Attachment: " + a.name() + " (" + buildAttachmentLink(a.url()) + ")")
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

    private String buildAttachmentLink(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return "";
        String base = (supportFrontendBaseUrl != null && !supportFrontendBaseUrl.isBlank())
                ? supportFrontendBaseUrl
                : (fallbackFrontendBaseUrl != null && !fallbackFrontendBaseUrl.isBlank() ? fallbackFrontendBaseUrl : "");
        if (base == null) base = "";
        String cleanBase = base.replaceAll("/$", "");
        try {
            String encoded = java.net.URLEncoder.encode(relativePath, java.nio.charset.StandardCharsets.UTF_8.toString());
            return cleanBase + "/api/attachments/file?path=" + encoded;
        } catch (Exception e) {
            return cleanBase + "/api/attachments/file?path=" + relativePath;
        }
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

    private boolean matchesAdminSourceFilter(String sourceFilter, SupportTicket ticket) {
        if (sourceFilter == null || sourceFilter.isBlank() || "all".equalsIgnoreCase(sourceFilter)) {
            return true;
        }
        String normalizedFilter = sourceFilter.trim().toLowerCase(Locale.ROOT);
        String ticketSource = ticket.getSource() != null ? ticket.getSource().trim().toLowerCase(Locale.ROOT) : "";

        if ("public".equals(normalizedFilter)) {
            return ticket.getUser() == null || "support_public".equals(ticketSource) || "public_app".equals(ticketSource) || "public".equals(ticketSource);
        }
        if ("login".equals(normalizedFilter) || "user".equals(normalizedFilter)) {
            return ticket.getUser() != null || "support_user".equals(ticketSource) || "app_user".equals(ticketSource) || "login".equals(ticketSource);
        }
        if ("admin".equals(normalizedFilter)) {
            return ticketSource.contains("admin");
        }
        return ticketSource.equals(normalizedFilter);
    }

    private boolean isPublicPortalSource(String source) {
        if (source == null || source.isBlank()) {
            return true;
        }
        String normalized = source.trim().toLowerCase(Locale.ROOT);
        return "support_public".equals(normalized) || "public_app".equals(normalized) || "public".equals(normalized);
    }

    private boolean isSupportAgent(User user) {
        if (user == null || !Boolean.TRUE.equals(user.getActive()) || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .map(role -> role == null ? "" : role.trim().toUpperCase(Locale.ROOT))
                .anyMatch(role -> "ADMIN".equals(role) || "SUPERADMIN".equals(role));
    }

    private Optional<String> findLeastLoadedActiveAgentEmail() {
        List<User> agents = userRepository.findAll().stream()
                .filter(this::isSupportAgent)
                .collect(Collectors.toList());
        if (agents.isEmpty()) {
            return Optional.empty();
        }

        return agents.stream()
                .min(Comparator.comparingLong(agent ->
                        ticketRepository.countByAssignedToAndStatusIn(agent.getEmail(), ACTIVE_TICKET_STATUSES)))
                .map(User::getEmail);
    }

    private Optional<String> assignTicketToAvailableAgent(SupportTicket ticket) {
        if (ticket == null || ticket.getAssignedTo() != null) {
            return Optional.empty();
        }
        Optional<String> agentEmail = findLeastLoadedActiveAgentEmail();
        if (agentEmail.isEmpty()) {
            return Optional.empty();
        }

        ticket.setAssignedTo(agentEmail.get());
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        return agentEmail;
    }

    private SupportTicket requirePublicAccessibleTicket(String displayId) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        if (!isPublicPortalSource(ticket.getSource()) || ticket.getUser() != null) {
            logger.warn("Blocked public ticket access for displayId={} source={} userPresent={}",
                    displayId, ticket.getSource(), ticket.getUser() != null);
            throw new ResourceNotFoundException("Ticket not found: " + displayId);
        }
        return ticket;
    }

    @Value("${farmeazy.app.support-base-url:${FARMEAZY_SUPPORT_BASE_URL:https://support.farm-eazy.com}}")
    private String supportFrontendBaseUrl;

    @Value("${farmeazy.app.public-base-url:${FARMEAZY_PUBLIC_BASE_URL:https://www.farm-eazy.com}}")
    private String publicFrontendBaseUrl;

    @Value("${farmeazy.app.base-url:${farmeazy.app.public-base-url:${FARMEAZY_PUBLIC_BASE_URL:https://www.farm-eazy.com}}}")
    private String fallbackFrontendBaseUrl;

    private String buildTicketUrl(String displayId, boolean isPublicTicket) {
        if (displayId == null || displayId.isBlank()) {
            displayId = "CHT00000";
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
        String nextDisplayId = sequenceGeneratorService.getNextSupportTicketDisplayId();
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

    private String escapeHtml(String value) {
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

    private String toHtmlLines(String value) {
        return escapeHtml(value).replace("\n", "<br>");
    }

    private String toHtmlWithAttachmentLinks(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String escaped = escapeHtml(value);
        String linked = escaped.replaceAll(
                "(?im)Attachment[s]?:\\s*([^\\n(]+?)\\s*\\((https?://[^)]+|/uploads/[^)]+)\\)",
                "<a href=\"$2\" target=\"_blank\" rel=\"noreferrer noopener\">$1</a>"
        );
        return linked.replace("\n", "<br>");
    }

    private String detailRow(String label, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "<tr>" +
                "<td style='padding:8px 0; vertical-align:top; color:#6b7280; font-weight:600; width:140px;'>" + escapeHtml(label) + "</td>" +
                "<td style='padding:8px 0; color:#111827; font-weight:500;'>" + toHtmlLines(value) + "</td>" +
                "</tr>";
    }

    private String buildTicketEmailContent(String intro, String detailRows, String updateTitle, String updateBody) {
        String detailsSection = (detailRows != null && !detailRows.isBlank())
                ? "<table style='width:100%; border-collapse:collapse; margin:16px 0 8px;'>" + detailRows + "</table>"
                : "";
        String updateSection = (updateBody != null && !updateBody.isBlank())
                ? "<div style='margin-top:16px; padding:14px 16px; border-radius:10px; background:#f8fafc; border:1px solid #e5e7eb;'>" +
                    "<p style='margin:0 0 8px; font-weight:700; color:#1f2937;'>" + escapeHtml(updateTitle != null ? updateTitle : "Update") + "</p>" +
                    "<p style='margin:0; color:#374151;'>" + toHtmlLines(updateBody) + "</p>" +
                  "</div>"
                : "";
        return "<p style='margin:0 0 14px; color:#374151;'>" + escapeHtml(intro) + "</p>" + detailsSection + updateSection;
    }

    private String loadTemplate(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            try (java.io.InputStream inputStream = resource.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load email template: " + classpathLocation, ex);
        }
    }

    private String replacePlaceholders(String template, Map<String, String> placeholders) {
        String rendered = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return rendered;
    }

    private String supportResponseTimeFor(SupportTicket ticket) {
        if (ticket == null || ticket.getPriority() == null) {
            return "48 hours";
        }
        return switch (ticket.getPriority()) {
            case URGENT -> "24 hours";
            case HIGH -> "24 hours";
            case MEDIUM -> "48 hours";
            case LOW -> "72 hours";
            default -> "48 hours";
        };
    }

    private String supportCreatedDateFor(SupportTicket ticket) {
        LocalDateTime createdAt = ticket != null && ticket.getCreatedAt() != null ? ticket.getCreatedAt() : LocalDateTime.now();
        return createdAt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    private String buildSupportIssueSummary(String issueLabel, String issueText, String updateLabel, String updateText) {
        StringBuilder html = new StringBuilder();
        if (issueText != null && !issueText.isBlank()) {
            html.append("<strong>")
                    .append(escapeHtml(issueLabel))
                    .append("</strong><br>")
                    .append(toHtmlWithAttachmentLinks(issueText));
        }
        if (updateText != null && !updateText.isBlank()) {
            if (!html.isEmpty()) {
                html.append("<br><br>");
            }
            html.append("<strong>")
                    .append(escapeHtml(updateLabel))
                    .append("</strong><br>")
                    .append(toHtmlWithAttachmentLinks(updateText));
        }
        return html.toString();
    }

    private String buildSupportTicketEmailHtml(SupportTicket ticket, String userName, String issueSummaryHtml, String ticketUrl) {
        return buildSupportTicketEmailHtml(ticket, userName, issueSummaryHtml, ticketUrl,
                "Support Ticket Update",
                "Your support ticket has been received. Our team is reviewing the latest update now."
        );
    }

    private String buildSupportTicketEmailHtml(SupportTicket ticket, String userName, String issueSummaryHtml, String ticketUrl, String emailHeading, String emailLeadText) {
        String template = loadTemplate("templates/emails/support-ticket.html");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("APP_NAME", "FarmEazy");
        placeholders.put("USER_NAME", userName != null && !userName.isBlank() ? userName : "User");
        placeholders.put("TICKET_ID", ticket != null && ticket.getDisplayId() != null ? ticket.getDisplayId() : "");
        placeholders.put("TICKET_TITLE", ticket != null && ticket.getSubject() != null ? ticket.getSubject() : "");
        placeholders.put("TICKET_CATEGORY", ticket != null && ticket.getCategory() != null ? String.valueOf(ticket.getCategory()) : "");
        placeholders.put("TICKET_PRIORITY", ticket != null && ticket.getPriority() != null ? String.valueOf(ticket.getPriority()) : "");
        placeholders.put("TICKET_STATUS", ticket != null && ticket.getStatus() != null ? String.valueOf(ticket.getStatus()) : "");
        placeholders.put("CREATED_DATE", supportCreatedDateFor(ticket));
        placeholders.put("ISSUE_DESCRIPTION", issueSummaryHtml != null ? issueSummaryHtml : "");
        placeholders.put("RESPONSE_TIME", supportResponseTimeFor(ticket));
        placeholders.put("TICKET_URL", ticketUrl != null ? ticketUrl : "");
        placeholders.put("EMAIL_HEADING", emailHeading != null && !emailHeading.isBlank() ? emailHeading : "Support Ticket Update");
        placeholders.put("EMAIL_LEAD", emailLeadText != null && !emailLeadText.isBlank() ? emailLeadText : "Your support ticket has been received. Our team is reviewing the latest update now.");
        placeholders.put("SUPPORT_EMAIL", "support@farm-eazy.com");
        placeholders.put("WEBSITE_URL", "https://farm-eazy.com");
        placeholders.put("YEAR", String.valueOf(java.time.Year.now().getValue()));
        return replacePlaceholders(template, placeholders);
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
            @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
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
                ticket.setDisplayId(sequenceGeneratorService.getNextSupportTicketDisplayId());
                Optional<String> assignedAgent = assignTicketToAvailableAgent(ticket);
                SupportTicket saved = ticketRepository.saveAndFlush(ticket);
                if (saved == null || saved.getId() == null) {
                    throw new IllegalStateException("Support ticket ID missing after save");
                }
                if (assignedAgent.isPresent()) {
                    createSupportTicketMessage(saved, "SYSTEM", "Auto Assign", "Ticket assigned to " + assignedAgent.get(), (String) null);
                }

                List<StoredAttachment> storedAttachments = storeAttachments(files);

                // Record initial user message in conversation (do not duplicate attachment lines in message)
                String initialBody = saved.getDescription();
                createSupportTicketMessage(saved, "USER", saved.getContactEmail(), initialBody, storedAttachments);

                logger.info("Created guest support ticket {}", saved.getDisplayId());
                String subject = "[Ticket #" + saved.getDisplayId() + "] " + saved.getSubject() + " – FarmEazy Support";
                String html = buildSupportTicketEmailHtml(
                    saved,
                    saved.getContactEmail(),
                    buildSupportIssueSummary("Issue description", saved.getDescription(), null, null),
                    buildTicketUrl(saved.getDisplayId(), true)
                );
                try {
                    emailService.sendEmail("support@farm-eazy.com", subject, html, UnifiedEmailService.SenderType.SUPPORT);
                } catch (Exception ex) {
                    logger.warn("Support notification email failed for guest ticket {}. Ticket remains created.", saved.getDisplayId(), ex);
                }

                // Notify user (confirmation email) - guest tickets should route to public ticket tracking
                String ticketUrl = buildTicketUrl(saved.getDisplayId(), true);
                String userSubject = "Your FarmEazy question has been received (" + saved.getDisplayId() + ")";
                String userHtml = buildEmailTemplate(
                    "Your support ticket is created",
                    buildTicketEmailContent(
                        "We received your request and our support team is reviewing it.",
                        detailRow("Ticket ID", saved.getDisplayId()) +
                            detailRow("Subject", saved.getSubject()) +
                            detailRow("Priority", String.valueOf(saved.getPriority())),
                        "What happens next",
                        "You can track this ticket and reply anytime from your support page. We will notify you when a support agent responds."
                    ),
                    "Track your ticket",
                    ticketUrl
                );
                emailService.sendEmail(saved.getContactEmail(), userSubject, userHtml, UnifiedEmailService.SenderType.SUPPORT);

                return SupportTicketResponseDto.fromEntity(saved);
            }
        @Autowired
        private SupportTicketRepository ticketRepository;
        @Autowired
        private com.farmeazy.repository.SupportTicketAttachmentRepository supportTicketAttachmentRepository;
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
    @Cacheable(cacheNames = "supportTicketList", key = "'admin:' + #page + ':' + #size + ':' + #status + ':' + #category + ':' + #priority + ':' + #important + ':' + #archived + ':' + #search + ':' + #source")
    public java.util.Map<String, Object> getAllTicketsFiltered(int page, int size, String status, String category, String priority, Boolean important, Boolean archived, String search, String source) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<SupportTicket> spec = Specification.where(null);

        if (status != null && !status.isBlank()) {
            try {
                SupportTicket.TicketStatus st = SupportTicket.TicketStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), st));
            } catch (Exception ignored) {
            }
        }

        if (category != null && !category.isBlank()) {
            try {
                SupportTicket.TicketCategory cat = SupportTicket.TicketCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
                spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), cat));
            } catch (Exception ignored) {
            }
        }

        if (priority != null && !priority.isBlank()) {
            try {
                SupportTicket.TicketPriority p = SupportTicket.TicketPriority.valueOf(priority.trim().toUpperCase(Locale.ROOT));
                spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), p));
            } catch (Exception ignored) {
            }
        }

        if (important != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("important"), important));
        }

        if (archived != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("archived"), archived));
        }

        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("displayId")), like),
                    cb.like(cb.lower(root.get("subject")), like),
                    cb.like(cb.lower(root.get("description")), like)
            ));
        }

        if (source != null && !source.isBlank() && !"all".equalsIgnoreCase(source)) {
            String normalized = source.trim().toLowerCase(Locale.ROOT);
            if ("public".equals(normalized)) {
                spec = spec.and((root, query, cb) -> cb.or(
                        cb.isNull(root.get("user")),
                        cb.equal(cb.lower(root.get("source")), "support_public"),
                        cb.equal(cb.lower(root.get("source")), "public_app"),
                        cb.equal(cb.lower(root.get("source")), "public")
                ));
            } else if ("login".equals(normalized) || "user".equals(normalized)) {
                spec = spec.and((root, query, cb) -> cb.or(
                        cb.isNotNull(root.get("user")),
                        cb.equal(cb.lower(root.get("source")), "support_user"),
                        cb.equal(cb.lower(root.get("source")), "app_user"),
                        cb.equal(cb.lower(root.get("source")), "login")
                ));
            } else if ("admin".equals(normalized)) {
                spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("source")), "%admin%"));
            } else {
                spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("source")), normalized));
            }
        }

        Page<SupportTicket> ticketPage = ticketRepository.findAll(spec, pageable);
        java.util.List<SupportTicketResponseDto> pageList = ticketPage.getContent().stream()
                .map(SupportTicketResponseDto::fromEntity)
                .collect(Collectors.toList());

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("tickets", pageList);
        result.put("total", ticketPage.getTotalElements());
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("source", source != null ? source : "all");
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

    private SupportTicket requireTicketAccess(String userEmail, String displayId) {
        User requester = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = resolveTicket(displayId);

        // Support agents can access all tickets.
        if (isSupportAgent(requester)) {
            return ticket;
        }

        // Authenticated owner can access own ticket.
        if (ticket.getUser() != null && requester.getId().equals(ticket.getUser().getId())) {
            return ticket;
        }

        // Legacy guest tickets can be accessed by the same contact email after login.
        if (ticket.getUser() == null
                && ticket.getContactEmail() != null
                && ticket.getContactEmail().equalsIgnoreCase(userEmail)) {
            return ticket;
        }

        throw new ResourceNotFoundException("Ticket not found: " + displayId);
    }

    @Transactional
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
    public SupportTicketResponseDto setImportant(String displayId, boolean important) {
        SupportTicket ticket = resolveTicket(displayId);
        ticket.setImportant(important);
        ticketRepository.save(ticket);

        createSupportTicketMessage(ticket, "SYSTEM", "System", "Important flag set to " + important + "", (String) null);

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
    public SupportTicketResponseDto setArchived(String displayId, boolean archived) {
        SupportTicket ticket = resolveTicket(displayId);
        ticket.setArchived(archived);
        ticketRepository.save(ticket);

        createSupportTicketMessage(ticket, "SYSTEM", "System", "Archived flag set to " + archived + "", (String) null);

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
        if (ticket == null || ticket.getId() == null) {
            throw new IllegalStateException("Cannot create support ticket message without a saved ticket ID");
        }
        SupportTicketMessage event = new SupportTicketMessage(ticket.getId(), senderType, senderName, message, attachmentUrl);
        supportTicketMessageRepository.save(event);
    }

    private void createSupportTicketMessage(SupportTicket ticket, String senderType, String senderName, String message, List<StoredAttachment> attachments) {
        if (ticket == null || ticket.getId() == null) {
            throw new IllegalStateException("Cannot create support ticket message without a saved ticket ID");
        }
        String primaryUrl = (attachments == null || attachments.isEmpty()) ? null : attachments.get(0).url();
        SupportTicketMessage event = new SupportTicketMessage(ticket.getId(), senderType, senderName, message, primaryUrl);
        SupportTicketMessage saved = supportTicketMessageRepository.save(event);
        if (attachments != null && !attachments.isEmpty()) {
            java.util.Set<String> savedUrls = new java.util.HashSet<>();
            for (StoredAttachment a : attachments) {
                if (a == null) continue;
                String url = a.url();
                if (url == null) continue;
                if (savedUrls.contains(url)) continue;
                savedUrls.add(url);
                com.farmeazy.entity.SupportTicketAttachment ta = new com.farmeazy.entity.SupportTicketAttachment(saved.getId(), a.name(), url);
                supportTicketAttachmentRepository.save(ta);
            }
        }
    }

    /**
     * ADMIN: Set ticket status (ADMIN or SUPERADMIN)
     */
    @Transactional
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
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
            createSupportTicketMessage(ticket, "SYSTEM", "System", statusChangeMessage, (String) null);

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
                .collect(Collectors.toCollection(ArrayList::new));

        // Populate attachments for each message from the attachments table
        for (com.farmeazy.dto.SupportTicketMessageDto dto : messages) {
            if (dto.getId() == null) continue;
            java.util.List<com.farmeazy.entity.SupportTicketAttachment> rows = supportTicketAttachmentRepository.findBySupportTicketMessageId(dto.getId());
            if (rows == null || rows.isEmpty()) {
                dto.setAttachments(null);
            } else {
                dto.setAttachments(rows.stream().map(com.farmeazy.dto.SupportTicketAttachmentDto::fromEntity).collect(Collectors.toList()));
            }
        }

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
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
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
        createSupportTicketMessage(ticket, "ADMIN", adminEmail, reply, (String) null);

        logger.info("Admin {} replied to ticket {}", adminEmail, displayId);

        // Notify user
        String userEmail = ticket.getContactEmail();
        String resolvedDisplayId = resolveDisplayId(ticket);
        boolean isPublicTicket = ticket.getUser() == null;
        String ticketUrl = buildTicketUrl(resolvedDisplayId, isPublicTicket);
            String subject = "[Ticket #" + resolvedDisplayId + "] " + ticket.getSubject() + " – FarmEazy Support";
            String html = buildSupportTicketEmailHtml(
                ticket,
                userEmail,
                buildSupportIssueSummary("Issue summary", ticket.getDescription(), "Latest update", reply),
                ticketUrl,
                "Support ticket reply",
                "A support executive has replied to your ticket. View the latest update below."
            );
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
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
    public SupportTicketResponseDto resolveTicketAdmin(String adminEmail, String displayId, String resolution) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        ticket.setResolution(resolution);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        createSupportTicketMessage(ticket, "SYSTEM", "System", "Ticket marked RESOLVED by " + adminEmail, (String) null);

        logger.info("Admin {} resolved ticket {}", adminEmail, displayId);
            // Trigger email notification to user
            String userEmail = ticket.getContactEmail();
            String resolvedDisplayId = resolveDisplayId(ticket);
            boolean isPublicTicket = ticket.getUser() == null;
            String ticketUrl = buildTicketUrl(resolvedDisplayId, isPublicTicket);
            String subject = "[Ticket #" + resolvedDisplayId + "] " + ticket.getSubject() + " – FarmEazy Support";
            String html = buildSupportTicketEmailHtml(
                ticket,
                userEmail,
                buildSupportIssueSummary("Issue summary", ticket.getDescription(), "Resolution summary", resolution),
                ticketUrl,
                "Ticket resolved",
                "Your ticket has been marked as resolved by our support team. Please review the resolution below."
            );
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

    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;

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
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
    public SupportTicketResponseDto adminUploadAttachment(String displayId, org.springframework.web.multipart.MultipartFile file, String adminEmail) {
        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));
        try {
            StoredAttachment storedAttachment = storeAttachment(file);
            String fileUrl = storedAttachment != null ? storedAttachment.url() : null;
            String fileName = storedAttachment != null ? storedAttachment.name() : (file != null ? file.getOriginalFilename() : "attachment");
            String notes = (ticket.getAdminNotes() != null ? ticket.getAdminNotes() : "") +
                    "\n\n--- Admin Attachment (" + java.time.LocalDateTime.now() + ", " + adminEmail + ") ---\n" +
                "Attachment: " + fileName + " (" + buildAttachmentLink(fileUrl) + ")\n";
            ticket.setAdminNotes(notes);
            ticket.setUpdatedAt(LocalDateTime.now());
            ticketRepository.save(ticket);

            createSupportTicketMessage(ticket, "ADMIN", adminEmail, "Attached file: " + fileName, java.util.List.of(storedAttachment));

            logger.info("Admin {} uploaded attachment to ticket {}", adminEmail, displayId);
            // Optionally notify user
            String userEmail = ticket.getContactEmail();
                String subject = "[Ticket #" + ticket.getDisplayId() + "] " + ticket.getSubject() + " – FarmEazy Support";
                String html = buildSupportTicketEmailHtml(
                    ticket,
                    userEmail,
                    buildSupportIssueSummary(
                        "Issue summary",
                        ticket.getDescription(),
                        "Attachment added",
                        fileUrl != null ? (fileName + " (" + buildAttachmentLink(fileUrl) + ")") : fileName
                    ),
                    buildTicketUrl(ticket.getDisplayId(), ticket.getUser() == null),
                    "Support ticket updated",
                    "A support executive attached a file to your ticket. Review the update below."
                );
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
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
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
        String notesAppend = appendAttachmentsToBody(replyText, storedAttachments);

        // Append admin reply and optional attachment to adminNotes (notes include attachment lines)
        String notes = (ticket.getAdminNotes() != null ? ticket.getAdminNotes() : "") +
            "\n\n--- Admin Reply (" + LocalDateTime.now() + ", " + adminEmail + ") ---\n" + notesAppend;
        ticket.setAdminNotes(notes);

        // Move status to IN_PROGRESS if appropriate
        if (ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.PENDING_USER) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // Add message row (message text only; attachments stored separately)
        createSupportTicketMessage(ticket, "ADMIN", adminEmail, replyText, storedAttachments);

        // Notify user once about reply and/or attachment
        try {
            String userEmail = ticket.getContactEmail();
            boolean isPublicTicket = ticket.getUser() == null;
            String ticketUrl = buildTicketUrl(ticket.getDisplayId(), isPublicTicket);
            String subject = "Support Ticket Update: " + ticket.getDisplayId();
                String attachmentNames = hasFile
                    ? storedAttachments.stream().map(StoredAttachment::name).collect(Collectors.joining(", "))
                    : null;
                String updateBody = hasReply ? reply : "A new attachment was added by support.";
                if (attachmentNames != null) {
                updateBody += "\n\nAttachments: " + attachmentNames;
                }
                String html = buildEmailTemplate(
                    "Support ticket updated",
                    buildTicketEmailContent(
                        "Your ticket has a new update from our support team.",
                        detailRow("Ticket ID", ticket.getDisplayId()) +
                            detailRow("Status", String.valueOf(ticket.getStatus())) +
                            detailRow("Attachment(s)", attachmentNames),
                        hasReply ? "Support reply" : "Attachment update",
                        updateBody
                    ),
                    "Open ticket",
                    ticketUrl
                );
                emailService.sendEmail(userEmail, subject, html, UnifiedEmailService.SenderType.SUPPORT);

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
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
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
        ticket.setContactEmail(dto.getContactEmail() != null && !dto.getContactEmail().isBlank() ? dto.getContactEmail() : user.getEmail());
        ticket.setContactPhone(dto.getContactPhone() != null && !dto.getContactPhone().isBlank() ? dto.getContactPhone() : user.getPhone());
        ticket.setOrderId(dto.getOrderId());
        ticket.setServiceId(dto.getServiceId());
        ticket.setDisplayId(sequenceGeneratorService.getNextSupportTicketDisplayId());
        Optional<String> assignedAgent = assignTicketToAvailableAgent(ticket);

        // Save to get ID
        SupportTicket saved = ticketRepository.saveAndFlush(ticket);
        if (saved == null || saved.getId() == null) {
            throw new IllegalStateException("Support ticket ID missing after save");
        }
        if (assignedAgent.isPresent()) {
            createSupportTicketMessage(saved, "SYSTEM", "Auto Assign", "Ticket assigned to " + assignedAgent.get(), (String) null);
        }

        List<StoredAttachment> storedAttachments = storeAttachments(files);

        // Record initial user message in conversation (do not duplicate attachment lines in message)
        String initialBody = saved.getDescription();
        createSupportTicketMessage(saved, "USER", saved.getContactEmail(), initialBody, storedAttachments);

        logger.info("Created support ticket {} for user {}", saved.getDisplayId(), userEmail);
        // Send email to support@farm-eazy.com
        String subject = "New Support Ticket: " + saved.getDisplayId() + " (" + saved.getSubject() + ")";
        String html = buildEmailTemplate(
            "New support ticket raised",
            buildTicketEmailContent(
                "A registered user submitted a new support ticket.",
                detailRow("Ticket ID", saved.getDisplayId()) +
                        detailRow("User", user.getEmail()) +
                        detailRow("Subject", saved.getSubject()) +
                        detailRow("Category", String.valueOf(saved.getCategory())) +
                        detailRow("Priority", String.valueOf(saved.getPriority())),
                "Issue description",
                saved.getDescription()
            ),
            "Open support dashboard",
            buildTicketUrl(saved.getDisplayId(), false)
        );
        try {
            emailService.sendEmail("support@farm-eazy.com", subject, html, UnifiedEmailService.SenderType.SUPPORT);
        } catch (Exception ex) {
            logger.warn("Support notification email failed for ticket {}. Ticket remains created.", saved.getDisplayId(), ex);
        }

        String resolvedDisplayId = resolveDisplayId(saved);
        String userTicketUrl = buildTicketUrl(resolvedDisplayId, false);
        String userSubject = "[Ticket #" + resolvedDisplayId + "] " + saved.getSubject() + " – FarmEazy Support";
        String userHtml = buildSupportTicketEmailHtml(
            saved,
            user.getUsername() != null ? user.getUsername() : user.getEmail(),
            buildSupportIssueSummary("Issue summary", saved.getDescription(), null, null),
            userTicketUrl
        );
        try {
            emailService.sendEmail(saved.getContactEmail(), userSubject, userHtml, UnifiedEmailService.SenderType.SUPPORT);
        } catch (Exception ex) {
            logger.warn("User confirmation email failed for ticket {}. Ticket remains created.", saved.getDisplayId(), ex);
        }

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
        SupportTicket ticket = requirePublicAccessibleTicket(displayId);
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.farmeazy.dto.SupportTicketMessageDto> getPublicTicketMessages(String displayId) {
        SupportTicket ticket = requirePublicAccessibleTicket(displayId);
        java.util.List<com.farmeazy.dto.SupportTicketMessageDto> messages = supportTicketMessageRepository
                .findBySupportTicketIdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(com.farmeazy.dto.SupportTicketMessageDto::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));

        // Populate attachments for each message from the attachments table (public access should still show attachments)
        for (com.farmeazy.dto.SupportTicketMessageDto dto : messages) {
            if (dto.getId() == null) continue;
            java.util.List<com.farmeazy.entity.SupportTicketAttachment> rows = supportTicketAttachmentRepository.findBySupportTicketMessageId(dto.getId());
            if (rows == null || rows.isEmpty()) {
                dto.setAttachments(null);
            } else {
                dto.setAttachments(rows.stream().map(com.farmeazy.dto.SupportTicketAttachmentDto::fromEntity).collect(Collectors.toList()));
            }
        }

        // Fallback legacy behavior: if no message rows exist, include description/admin notes similar to authenticated view
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
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
    public SupportTicketResponseDto addPublicResponseWithAttachments(String displayId, String response, String senderEmail, List<MultipartFile> files) {
        boolean hasResponse = response != null && !response.isBlank();
        List<StoredAttachment> storedAttachments = storeAttachments(files);
        if (!hasResponse && storedAttachments.isEmpty()) {
            throw new IllegalArgumentException("Response message or attachment is required");
        }

        SupportTicket ticket = requirePublicAccessibleTicket(displayId);

        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot respond to a closed/cancelled ticket");
        }

        String sender = senderEmail != null ? senderEmail : ticket.getContactEmail();
        String messageBody = hasResponse ? response : "Attachment added";
        // Do not embed attachment lines into the conversation message to avoid duplicates
        createSupportTicketMessage(ticket, "USER", sender, messageBody, storedAttachments);

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // Notify support team about user reply
        String supportSubject = "Public ticket response received: " + ticket.getDisplayId();
        String userLink = buildTicketUrl(ticket.getDisplayId(), true);
        String supportHtml = buildEmailTemplate(
            "User replied on public ticket",
            buildTicketEmailContent(
                "A customer sent a new message on a public support ticket.",
                detailRow("Ticket ID", ticket.getDisplayId()) +
                    detailRow("From", sender) +
                    detailRow("Status", String.valueOf(ticket.getStatus())),
                "User message",
                messageBody
            ),
            "Open public ticket",
            userLink
        );
        emailService.sendEmail("support@farm-eazy.com", supportSubject, supportHtml, UnifiedEmailService.SenderType.SUPPORT);

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
    public SupportTicketResponseDto reopenPublicTicket(String displayId, String requesterEmail) {
        SupportTicket ticket = requirePublicAccessibleTicket(displayId);

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reopen a cancelled ticket");
        }

        ticket.setStatus(TicketStatus.OPEN);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        createSupportTicketMessage(ticket, "SYSTEM", "System", "Ticket reopened by " + (requesterEmail != null ? requesterEmail : "public user"), (String) null);

        String userLink = buildTicketUrl(ticket.getDisplayId(), true);
        String userSubject = "[Ticket #" + ticket.getDisplayId() + "] " + ticket.getSubject() + " – FarmEazy Support";
        String userHtml = buildSupportTicketEmailHtml(
            ticket,
            requesterEmail != null ? requesterEmail : (ticket.getContactEmail() != null ? ticket.getContactEmail() : "User"),
            buildSupportIssueSummary(
                "Issue summary",
                ticket.getDescription(),
                "Latest update",
                "Your request is active again and our team will continue helping you. You can add more information or attachments from your ticket thread."
            ),
            userLink
        );
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
        SupportTicket ticket = requireTicketAccess(userEmail, displayId);
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.farmeazy.dto.SupportTicketMessageDto> getTicketMessagesForUser(String userEmail, String displayId) {
        SupportTicket ticket = requireTicketAccess(userEmail, displayId);
        java.util.List<com.farmeazy.dto.SupportTicketMessageDto> messages = supportTicketMessageRepository
                .findBySupportTicketIdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(com.farmeazy.dto.SupportTicketMessageDto::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));

        for (com.farmeazy.dto.SupportTicketMessageDto dto : messages) {
            if (dto.getId() == null) continue;
            java.util.List<com.farmeazy.entity.SupportTicketAttachment> rows = supportTicketAttachmentRepository.findBySupportTicketMessageId(dto.getId());
            if (rows == null || rows.isEmpty()) {
                dto.setAttachments(null);
            } else {
                dto.setAttachments(rows.stream().map(com.farmeazy.dto.SupportTicketAttachmentDto::fromEntity).collect(Collectors.toList()));
            }
        }

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
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
    public SupportTicketResponseDto cancelTicket(String userEmail, String displayId) {
        SupportTicket ticket = requireTicketAccess(userEmail, displayId);

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
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
    public SupportTicketResponseDto addResponseWithAttachments(String userEmail, String displayId, String response, List<MultipartFile> files) {
        SupportTicket ticket = requireTicketAccess(userEmail, displayId);

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
        if (!storedAttachments.isEmpty()) {
            String attachmentLines = storedAttachments.stream()
                .map(a -> "Attachment: " + a.name() + " (" + buildAttachmentLink(a.url()) + ")")
                .collect(Collectors.joining("\n"));
            updatedDescription += "\n" + attachmentLines;
        }
        ticket.setDescription(updatedDescription);

        // Persist the response as a conversation message (text only); attachments stored separately.
        createSupportTicketMessage(ticket, "USER", userEmail, responseText, storedAttachments);

        // For email/notification bodies include attachment lines
        String responseForEmail = appendAttachmentsToBody(responseText, storedAttachments);
        
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
            String supportHtml = buildEmailTemplate(
                "User replied on support ticket",
                buildTicketEmailContent(
                    "A registered user posted a new response in an active ticket.",
                    detailRow("Ticket ID", resolveDisplayId(ticket)) +
                        detailRow("User", userEmail) +
                        detailRow("Status", String.valueOf(ticket.getStatus())),
                    "User message",
                    responseForEmail
                ),
                "Open ticket",
                buildTicketUrl(resolveDisplayId(ticket), false)
            );
            emailService.sendEmail("support@farm-eazy.com", supportSubject, supportHtml, UnifiedEmailService.SenderType.SUPPORT);
        } catch (Exception ex) {
            logger.warn("Failed sending support email notification for ticket user reply {}", displayId, ex);
        }

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
    public SupportTicketResponseDto reopenTicket(String userEmail, String displayId) {
        SupportTicket ticket = requireTicketAccess(userEmail, displayId);

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot reopen a cancelled ticket");
        }

        ticket.setStatus(TicketStatus.OPEN);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        createSupportTicketMessage(ticket, "SYSTEM", "System", "Ticket reopened by " + userEmail, (String) null);

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional
    @CacheEvict(cacheNames = {"supportTicketList", "supportTicketAdminStats", "supportTicketUserStats"}, allEntries = true)
    public SupportTicketResponseDto assignTicket(String displayId, String assigneeEmail, String requestedByEmail) {
        SupportTicket ticket = resolveTicket(displayId);

        User assignee = userRepository.findByEmail(assigneeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + assigneeEmail));

        if (!isSupportAgent(assignee)) {
            throw new IllegalArgumentException("Assignee must be an active ADMIN or SUPERADMIN user");
        }

        ticket.setAssignedTo(assignee.getEmail());
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        createSupportTicketMessage(ticket, "SYSTEM", "Assignment", "Ticket assigned to " + assignee.getEmail() + " by " + requestedByEmail, (String) null);
        return SupportTicketResponseDto.fromEntity(ticket);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "supportTicketAdminStats", key = "'global'", unless = "#result == null")
    public Map<String, Object> getAdminChatStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long totalTickets = ticketRepository.count();
        long openTickets = ticketRepository.countByStatus(TicketStatus.OPEN);
        long inProgressTickets = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long pendingUserTickets = ticketRepository.countByStatus(TicketStatus.PENDING_USER);
        long resolvedTickets = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        long closedTickets = ticketRepository.countByStatus(TicketStatus.CLOSED);
        long unassignedActiveTickets = ticketRepository.countByAssignedToIsNullAndStatusIn(ACTIVE_TICKET_STATUSES);
        long assignedActiveTickets = ticketRepository.countByAssignedToIsNotNullAndStatusIn(ACTIVE_TICKET_STATUSES);

        List<String> activeAgents = userRepository.findAll().stream()
                .filter(this::isSupportAgent)
                .map(User::getEmail)
                .collect(Collectors.toList());

        stats.put("totalTickets", totalTickets);
        stats.put("openTickets", openTickets);
        stats.put("inProgressTickets", inProgressTickets);
        stats.put("pendingUserTickets", pendingUserTickets);
        stats.put("resolvedTickets", resolvedTickets);
        stats.put("closedTickets", closedTickets);
        stats.put("assignedActiveTickets", assignedActiveTickets);
        stats.put("unassignedActiveTickets", unassignedActiveTickets);
        stats.put("activeAgents", activeAgents.size());
        stats.put("activeAgentEmails", activeAgents);
        return stats;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "supportTicketUserStats", key = "#userEmail", unless = "#result == null")
    public Map<String, Object> getUserChatStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, Object> stats = new LinkedHashMap<>();
        long totalTickets = ticketRepository.countByUser(user);
        long activeTickets = ticketRepository.countByUserAndStatusIn(user, ACTIVE_TICKET_STATUSES);
        long resolvedTickets = ticketRepository.countByUserAndStatus(user, TicketStatus.RESOLVED);
        long closedTickets = ticketRepository.countByUserAndStatus(user, TicketStatus.CLOSED);

        stats.put("totalTickets", totalTickets);
        stats.put("activeTickets", activeTickets);
        stats.put("resolvedTickets", resolvedTickets);
        stats.put("closedTickets", closedTickets);
        return stats;
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

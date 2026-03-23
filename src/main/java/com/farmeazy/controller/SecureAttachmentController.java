package com.farmeazy.controller;

import com.farmeazy.entity.FAQQuestion;
import com.farmeazy.entity.SupportTicket;
import com.farmeazy.entity.SupportTicketMessage;
import com.farmeazy.entity.User;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.FAQCommunicationRepository;
import com.farmeazy.repository.FAQQuestionRepository;
import com.farmeazy.repository.SupportTicketMessageRepository;
import com.farmeazy.repository.SupportTicketRepository;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.FileStorageService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/api/attachments")
public class SecureAttachmentController {

    private static final String UPLOAD_PREFIX = "/uploads/";

    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final FAQQuestionRepository faqQuestionRepository;
    private final FAQCommunicationRepository faqCommunicationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketMessageRepository supportTicketMessageRepository;

    public SecureAttachmentController(
            FileStorageService fileStorageService,
            UserRepository userRepository,
            FAQQuestionRepository faqQuestionRepository,
            FAQCommunicationRepository faqCommunicationRepository,
            SupportTicketRepository supportTicketRepository,
            SupportTicketMessageRepository supportTicketMessageRepository) {
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.faqQuestionRepository = faqQuestionRepository;
        this.faqCommunicationRepository = faqCommunicationRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.supportTicketMessageRepository = supportTicketMessageRepository;
    }

    @GetMapping("/file")
    public ResponseEntity<org.springframework.core.io.Resource> getAttachment(
            @RequestParam("path") String path,
            @RequestParam(value = "download", defaultValue = "false") boolean download,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Authentication is required to access attachments");
        }

        String normalizedPath = normalizeUploadPath(path);
        if (normalizedPath == null) {
            throw new UnauthorizedException("Invalid attachment path");
        }

        String currentEmail = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream().anyMatch(a -> {
            String role = a.getAuthority();
            return "ROLE_ADMIN".equals(role) || "ROLE_SUPERADMIN".equals(role);
        });

        if (!canAccessAttachment(normalizedPath, currentEmail, isAdmin)) {
            throw new UnauthorizedException("You are not allowed to access this attachment");
        }

        String filename = normalizedPath.substring(UPLOAD_PREFIX.length());
        org.springframework.core.io.Resource resource = fileStorageService.loadAsResource(filename);

        String mimeType = java.net.URLConnection.guessContentTypeFromName(filename);
        if (!StringUtils.hasText(mimeType)) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        ContentDisposition disposition = download
                ? ContentDisposition.attachment().filename(filename).build()
                : ContentDisposition.inline().filename(filename).build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    private boolean canAccessAttachment(String path, String email, boolean isAdmin) {
        if (isAdmin) {
            return existsInAnyKnownRecord(path);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        String userId = user != null ? String.valueOf(user.getId()) : null;

        return existsInUserFaqRecords(path, email, userId) || existsInUserTicketRecords(path, email, user);
    }

    private boolean existsInAnyKnownRecord(String path) {
        return faqQuestionRepository.existsByQuestionContaining(path)
                || faqCommunicationRepository.existsByBodyContaining(path)
                || supportTicketMessageRepository.existsByAttachmentUrl(path)
                || supportTicketMessageRepository.existsByMessageContaining(path);
    }

    private boolean existsInUserFaqRecords(String path, String email, String userId) {
        List<FAQQuestion> ownedFaqs;
        if (StringUtils.hasText(userId)) {
            ownedFaqs = faqQuestionRepository.findByEmailOrUserIdOrderBySubmittedAtDesc(email, userId);
        } else {
            ownedFaqs = faqQuestionRepository.findByEmailOrderBySubmittedAtDesc(email);
        }

        if (ownedFaqs.isEmpty()) {
            return false;
        }

        for (FAQQuestion faq : ownedFaqs) {
            if (StringUtils.hasText(faq.getQuestion()) && faq.getQuestion().contains(path)) {
                return true;
            }
            if (faqCommunicationRepository.findByFaqQuestionIdOrderBySentAtAsc(faq.getId())
                    .stream()
                    .anyMatch(comm -> StringUtils.hasText(comm.getBody()) && comm.getBody().contains(path))) {
                return true;
            }
        }

        return false;
    }

    private boolean existsInUserTicketRecords(String path, String email, User user) {
        Set<Long> ticketIds = new HashSet<>();
        List<SupportTicket> ownedTickets = new ArrayList<>();

        if (user != null) {
            ownedTickets.addAll(supportTicketRepository.findByUserOrderByCreatedAtDesc(user));
        }
        ownedTickets.addAll(supportTicketRepository.findByContactEmailOrderByCreatedAtDesc(email));

        for (SupportTicket ticket : ownedTickets) {
            ticketIds.add(ticket.getId());
            if (StringUtils.hasText(ticket.getDescription()) && ticket.getDescription().contains(path)) {
                return true;
            }
            if (StringUtils.hasText(ticket.getAdminNotes()) && ticket.getAdminNotes().contains(path)) {
                return true;
            }
        }

        if (ticketIds.isEmpty()) {
            return false;
        }

        List<SupportTicketMessage> messages = supportTicketMessageRepository
                .findBySupportTicketIdInOrderByCreatedAtAsc(new ArrayList<>(ticketIds));

        for (SupportTicketMessage message : messages) {
            if (path.equals(message.getAttachmentUrl())) {
                return true;
            }
            if (StringUtils.hasText(message.getMessage()) && message.getMessage().contains(path)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeUploadPath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return null;
        }

        String decoded = URLDecoder.decode(rawPath.trim(), StandardCharsets.UTF_8);

        if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
            try {
                java.net.URI uri = java.net.URI.create(decoded);
                decoded = uri.getPath();
            } catch (Exception ex) {
                return null;
            }
        }

        int queryIdx = decoded.indexOf('?');
        if (queryIdx >= 0) {
            decoded = decoded.substring(0, queryIdx);
        }

        if (!decoded.startsWith(UPLOAD_PREFIX)) {
            return null;
        }

        String filename = decoded.substring(UPLOAD_PREFIX.length());
        if (!StringUtils.hasText(filename) || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return null;
        }

        return UPLOAD_PREFIX + filename;
    }
}

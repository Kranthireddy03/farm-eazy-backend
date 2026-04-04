package com.farmeazy.controller;

import com.farmeazy.dto.SupportTicketDto;
import com.farmeazy.dto.SupportTicketResponseDto;
import com.farmeazy.entity.SupportTicket;
import com.farmeazy.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/support-tickets", "/api/support-tickets"})
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
@Tag(name = "Support Tickets", description = "Endpoints for users to create and manage their own support tickets")
public class SupportTicketController {
    private static final Logger logger = LoggerFactory.getLogger(SupportTicketController.class);

    private SupportTicket.TicketCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) return SupportTicket.TicketCategory.GENERAL;
        try {
            return SupportTicket.TicketCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SupportTicket.TicketCategory.GENERAL;
        }
    }

    private SupportTicket.TicketPriority parsePriority(String raw) {
        if (raw == null || raw.isBlank()) return SupportTicket.TicketPriority.MEDIUM;
        try {
            return SupportTicket.TicketPriority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SupportTicket.TicketPriority.MEDIUM;
        }
    }

    private List<MultipartFile> resolveFiles(MultipartFile[] files, MultipartFile file) {
        List<MultipartFile> merged = new ArrayList<>();
        if (files != null) {
            for (MultipartFile item : files) {
                if (item != null && !item.isEmpty()) {
                    merged.add(item);
                }
            }
        }
        if (file != null && !file.isEmpty()) {
            merged.add(file);
        }
        return merged.isEmpty() ? Collections.emptyList() : merged;
    }

    @PostMapping("/guest")
    @Operation(summary = "Create guest ticket", description = "Create a new support ticket for a guest user (no authentication required)")
    public ResponseEntity<SupportTicketResponseDto> createGuestTicket(@Valid @RequestBody SupportTicketDto dto) {
        logger.info("SUPPORT_CONTROLLER_CREATE_GUEST_TICKET email={}", dto != null ? dto.getContactEmail() : null);
        if (dto.getContactEmail() == null || dto.getContactEmail().isBlank()) {
            throw new com.farmeazy.exception.ResourceNotFoundException("Contact email is required for guest ticket");
        }
        SupportTicketResponseDto created = supportTicketService.createGuestTicket(dto);
        return ResponseEntity.ok(created);
    }

    @PostMapping(value = "/guest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create guest ticket with optional attachment", description = "Create a new support ticket for a guest user with optional file")
    public ResponseEntity<SupportTicketResponseDto> createGuestTicketMultipart(
            @RequestParam String subject,
            @RequestParam String description,
            @RequestParam String contactEmail,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String roleRequest,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        logger.info("SUPPORT_CONTROLLER_CREATE_GUEST_TICKET_MULTIPART email={} source={}", contactEmail, source);

        if (!resolveFiles(files, file).isEmpty()) {
            throw new IllegalArgumentException("Attachments are disabled for public users. Please sign in to attach files.");
        }

        SupportTicketDto dto = new SupportTicketDto();
        dto.setSubject(subject);
        dto.setDescription(description);
        dto.setContactEmail(contactEmail);
        dto.setContactPhone(contactPhone);
        dto.setCategory(parseCategory(category));
        dto.setPriority(parsePriority(priority));
        dto.setOrderId(orderId);
        dto.setServiceId(serviceId);
        dto.setSource(source);
        dto.setRoleRequest(roleRequest);

        SupportTicketResponseDto created = supportTicketService.createGuestTicket(dto);
        return ResponseEntity.ok(created);
    }

    // DEBUG: log raw body to help identify JSON parsing issues from clients.
    // Remove after issue is resolved.
    @PostMapping("/guest-debug")
    public ResponseEntity<String> debugGuestPayload(@RequestBody String body) {
        logger.debug("SUPPORT_CONTROLLER_GUEST_DEBUG payload={}", body);
        return ResponseEntity.ok("received");
    }

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private Environment env;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping
    @Operation(summary = "Create ticket", description = "Create a new support ticket for the authenticated user")
    public ResponseEntity<SupportTicketResponseDto> createTicket(@Valid @RequestBody SupportTicketDto dto, Authentication authentication) {
        String email = authentication.getName();
        logger.info("SUPPORT_CONTROLLER_CREATE_TICKET userEmail={} source={}", email, dto != null ? dto.getSource() : null);
        SupportTicketResponseDto created = supportTicketService.createTicket(email, dto);
        return ResponseEntity.ok(created);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create ticket with optional attachment", description = "Create a new support ticket for the authenticated user with optional file")
    public ResponseEntity<SupportTicketResponseDto> createTicketMultipart(
            @RequestParam String subject,
            @RequestParam String description,
            @RequestParam(required = false) String contactEmail,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String roleRequest,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {

        logger.info("SUPPORT_CONTROLLER_CREATE_TICKET_MULTIPART userEmail={} source={}", authentication != null ? authentication.getName() : null, source);

        SupportTicketDto dto = new SupportTicketDto();
        dto.setSubject(subject);
        dto.setDescription(description);
        dto.setContactEmail(contactEmail);
        dto.setContactPhone(contactPhone);
        dto.setCategory(parseCategory(category));
        dto.setPriority(parsePriority(priority));
        dto.setOrderId(orderId);
        dto.setServiceId(serviceId);
        dto.setSource(source);
        dto.setRoleRequest(roleRequest);

        String email = authentication.getName();
        SupportTicketResponseDto created = supportTicketService.createTicketWithAttachments(email, dto, resolveFiles(files, file));
        return ResponseEntity.ok(created);
    }

    @GetMapping
    @Operation(summary = "List tickets", description = "List tickets for the authenticated user; SUPERADMIN sees all tickets")
    public ResponseEntity<?> listTickets(Authentication authentication) {
        boolean isSuper = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
        logger.info("SUPPORT_CONTROLLER_LIST_TICKETS user={} isSuper={}", authentication.getName(), isSuper);
        if (isSuper) {
            List<SupportTicketResponseDto> all = supportTicketService.getAllTickets();
            return ResponseEntity.ok(all);
        }
        List<SupportTicketResponseDto> userTickets = supportTicketService.getUserTickets(authentication.getName());
        return ResponseEntity.ok(userTickets);
    }

    @GetMapping("/{displayId}")
    @Operation(summary = "Get ticket", description = "Get ticket details. Users can only access their own tickets; SUPERADMIN can access any ticket")
    public ResponseEntity<SupportTicketResponseDto> getTicket(@PathVariable String displayId, Authentication authentication) {
        boolean isSuper = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
        logger.info("SUPPORT_CONTROLLER_GET_TICKET displayId={} user={} isSuper={}", displayId, authentication.getName(), isSuper);
        if (isSuper) {
            return ResponseEntity.ok(supportTicketService.getTicketByDisplayIdAdmin(displayId));
        }
        return ResponseEntity.ok(supportTicketService.getTicketByDisplayId(authentication.getName(), displayId));
    }

    @GetMapping("/{displayId}/messages")
    @Operation(summary = "Get ticket messages (authenticated)", description = "Get authenticated ticket conversation messages")
    public ResponseEntity<?> getTicketMessages(@PathVariable String displayId, Authentication authentication) {
        logger.info("SUPPORT_CONTROLLER_GET_TICKET_MESSAGES displayId={} user={}", displayId, authentication != null ? authentication.getName() : null);
        java.util.List<com.farmeazy.dto.SupportTicketMessageDto> messages = supportTicketService.getTicketMessages(displayId);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("messages", messages);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/public/{displayId}")
    @Operation(summary = "Get public ticket", description = "Get public ticket details by displayId")
    public ResponseEntity<SupportTicketResponseDto> getPublicTicket(@PathVariable String displayId) {
        logger.info("SUPPORT_CONTROLLER_GET_PUBLIC_TICKET displayId={}", displayId);
        return ResponseEntity.ok(supportTicketService.getPublicTicketByDisplayId(displayId));
    }

    @GetMapping("/public/{displayId}/messages")
    @Operation(summary = "Get public ticket messages", description = "Get public ticket conversation messages")
    public ResponseEntity<?> getPublicTicketMessages(@PathVariable String displayId) {
        logger.info("SUPPORT_CONTROLLER_GET_PUBLIC_TICKET_MESSAGES displayId={}", displayId);
        java.util.List<com.farmeazy.dto.SupportTicketMessageDto> messages = supportTicketService.getPublicTicketMessages(displayId);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("messages", messages);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/public/{displayId}/reply")
    @Operation(summary = "Public write reply", description = "Public user adds a response to a ticket")
    public ResponseEntity<SupportTicketResponseDto> publicReply(@PathVariable String displayId, @RequestBody java.util.Map<String, String> body) {
        String response = body.getOrDefault("response", "");
        String email = body.getOrDefault("email", "public@anonymous.com");
        logger.info("SUPPORT_CONTROLLER_PUBLIC_REPLY displayId={} senderEmail={}", displayId, email);
        return ResponseEntity.ok(supportTicketService.addPublicResponse(displayId, response, email));
    }

    @PostMapping(path = "/public/{displayId}/reply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Public write reply with attachment", description = "Public user adds a response with optional file")
    public ResponseEntity<SupportTicketResponseDto> publicReplyMultipart(
            @PathVariable String displayId,
            @RequestParam(required = false) String response,
            @RequestParam(required = false) String email,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        logger.info("SUPPORT_CONTROLLER_PUBLIC_REPLY_MULTIPART displayId={} senderEmail={}", displayId, email);
        if (!resolveFiles(files, file).isEmpty()) {
            throw new IllegalArgumentException("Attachments are disabled for public users. Please sign in to attach files.");
        }
        String senderEmail = (email != null && !email.isBlank()) ? email : "public@anonymous.com";
        return ResponseEntity.ok(supportTicketService.addPublicResponse(displayId, response, senderEmail));
    }

    @PostMapping("/public/{displayId}/reopen")
    @Operation(summary = "Public reopen ticket", description = "Public user requests to reopen a ticket")
    public ResponseEntity<SupportTicketResponseDto> publicReopen(@PathVariable String displayId, @RequestBody(required = false) java.util.Map<String, String> body) {
        String email = body != null ? body.getOrDefault("email", "public@anonymous.com") : "public@anonymous.com";
        logger.info("SUPPORT_CONTROLLER_PUBLIC_REOPEN displayId={} requester={}", displayId, email);
        return ResponseEntity.ok(supportTicketService.reopenPublicTicket(displayId, email));
    }

    @PostMapping("/{displayId}/respond")
    @Operation(summary = "User respond", description = "Add a user response to their own ticket")
    public ResponseEntity<SupportTicketResponseDto> respondToTicket(@PathVariable String displayId, @RequestBody java.util.Map<String, String> body, Authentication authentication) {
        String resp = body.getOrDefault("response", "");
        logger.info("SUPPORT_CONTROLLER_USER_RESPOND displayId={} user={}", displayId, authentication.getName());
        SupportTicketResponseDto updated = supportTicketService.addResponse(authentication.getName(), displayId, resp);
        return ResponseEntity.ok(updated);
    }

    @PostMapping(path = "/{displayId}/respond", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "User respond with attachment", description = "Add a user response with optional file")
    public ResponseEntity<SupportTicketResponseDto> respondToTicketMultipart(
            @PathVariable String displayId,
            @RequestParam(required = false) String response,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        logger.info("SUPPORT_CONTROLLER_USER_RESPOND_MULTIPART displayId={} user={}", displayId, authentication.getName());
        SupportTicketResponseDto updated = supportTicketService.addResponseWithAttachments(authentication.getName(), displayId, response, resolveFiles(files, file));
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{displayId}/cancel")
    @Operation(summary = "Cancel ticket", description = "Cancel a ticket owned by the authenticated user")
    public ResponseEntity<SupportTicketResponseDto> cancelTicket(@PathVariable String displayId, Authentication authentication) {
        logger.info("SUPPORT_CONTROLLER_CANCEL_TICKET displayId={} user={}", displayId, authentication.getName());
        SupportTicketResponseDto updated = supportTicketService.cancelTicket(authentication.getName(), displayId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/count/active")
    @Operation(summary = "Active ticket count", description = "Return count of active tickets for the authenticated user")
    public ResponseEntity<java.util.Map<String, Object>> activeCount(Authentication authentication) {
        logger.info("SUPPORT_CONTROLLER_ACTIVE_COUNT user={}", authentication.getName());
        long c = supportTicketService.getActiveTicketCount(authentication.getName());
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("activeTickets", c);
        return ResponseEntity.ok(m);
    }

    @GetMapping("/stats/chat")
    @Operation(summary = "Authenticated user chat stats", description = "Return chat ticket statistics for the logged-in user")
    public ResponseEntity<java.util.Map<String, Object>> userChatStats(Authentication authentication) {
        logger.info("SUPPORT_CONTROLLER_CHAT_STATS user={}", authentication.getName());
        return ResponseEntity.ok(supportTicketService.getUserChatStats(authentication.getName()));
    }

    // Debug endpoint to inspect active spring profiles and JPA settings
    @GetMapping("/debug/properties")
    public ResponseEntity<Map<String, Object>> debugProperties() {
        logger.debug("SUPPORT_CONTROLLER_DEBUG_PROPERTIES");
        Map<String, Object> m = new HashMap<>();
        m.put("activeProfiles", env.getActiveProfiles());
        m.put("spring.jpa.hibernate.ddl-auto", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        m.put("spring.jpa.generate-ddl", env.getProperty("spring.jpa.generate-ddl"));
        m.put("spring.datasource.url", env.getProperty("spring.datasource.url"));
        m.put("spring.jpa.database-platform", env.getProperty("spring.jpa.database-platform"));
        return ResponseEntity.ok(m);
    }

    @GetMapping("/debug/tables")
    public ResponseEntity<Map<String, Object>> debugTables() {
        logger.debug("SUPPORT_CONTROLLER_DEBUG_TABLES");
        Map<String, Object> m = new HashMap<>();
        m.put("support_tickets_table_count", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
            Integer.class,
            "SUPPORT_TICKETS"
        ));
        m.put("all_tables", jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'"
        ));
        return ResponseEntity.ok(m);
    }
}

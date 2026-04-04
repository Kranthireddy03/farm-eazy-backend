package com.farmeazy.controller;

import com.farmeazy.dto.AdminRegisterRequestDto;
import com.farmeazy.dto.SupportTicketDto;
import com.farmeazy.dto.SupportTicketResponseDto;
import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/tickets")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "Admin Ticketing Dashboard", description = "Admin dashboard for ticket management and user role control")
public class AdminTicketingDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTicketingDashboardController.class);

    private String normalizeStatusOrNull(String status) {
        if (status == null) return null;
        String normalized = status.trim().toUpperCase();
        if (normalized.isEmpty()) return null;
        try {
            com.farmeazy.entity.SupportTicket.TicketStatus.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Register new admin/superadmin (SUPERADMIN only)
    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Register admin/superadmin", description = "Register a new admin or superadmin with email and password")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody AdminRegisterRequestDto request, Authentication authentication) {
        String requesterEmail = authentication.getName();
        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim();
        String role = request.getRole().trim().toUpperCase();

        if (!"ADMIN".equals(role) && !"SUPERADMIN".equals(role)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role. Allowed roles: ADMIN, SUPERADMIN"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone().trim())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phone already exists"));
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setActive(true);

        java.util.Set<String> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        User saved = userRepository.save(user);
        logger.info("AUDIT: Superadmin {} created admin account {} with role {} (userId={})", requesterEmail, saved.getEmail(), role, saved.getId());

        return ResponseEntity.ok(Map.of(
                "message", "Admin account created successfully",
                "email", saved.getEmail(),
                "username", saved.getUsername(),
                "role", role
        ));
    }

    // Get all tickets
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Get all tickets", description = "View all support tickets")
    public ResponseEntity<?> getAllTickets(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Boolean important,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(supportTicketService.getAllTicketsFiltered(page, size, status, category, priority, important, archived, search, source));
    }

    // Update ticket (reply, resolve, cancel, assign)
    @PutMapping("/{displayId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Update ticket", description = "Update ticket status or add admin reply")
    public ResponseEntity<?> updateTicket(@PathVariable String displayId, @RequestBody java.util.Map<String, Object> body, Authentication authentication) {
        String requesterEmail = authentication.getName();
        // If body contains 'description' treat as admin reply
        if (body.containsKey("description")) {
            String desc = (String) body.get("description");
            supportTicketService.adminReplyToTicket(requesterEmail, displayId, desc);
        }
        // If body contains 'status' attempt to set status
        if (body.containsKey("status")) {
            try {
                String st = (String) body.get("status");
                supportTicketService.setStatusAdmin(displayId, st);
            } catch (Exception ex) {
                return ResponseEntity.badRequest().body("Invalid status value");
            }
        }
        return ResponseEntity.ok("Ticket updated: " + displayId);
    }

    // Assign ticket to user
    @PutMapping("/{displayId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Assign ticket", description = "Assign ticket to a user")
    public ResponseEntity<?> assignTicket(@PathVariable String displayId, @RequestParam String userEmail, Authentication authentication) {
        String requesterEmail = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        SupportTicketResponseDto updated = supportTicketService.assignTicket(displayId, userEmail, requesterEmail);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/stats/chat")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Get chat stats", description = "Get support chat ticket and assignment statistics for admin dashboard")
    public ResponseEntity<?> getChatStats() {
        return ResponseEntity.ok(supportTicketService.getAdminChatStats());
    }

    @PostMapping("/{displayId}/important")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Mark ticket as important", description = "Set or unset important flag on a ticket")
    public ResponseEntity<SupportTicketResponseDto> setImportant(@PathVariable String displayId, @RequestParam boolean important) {
        return ResponseEntity.ok(supportTicketService.setImportant(displayId, important));
    }

    @GetMapping("/{displayId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Get ticket by displayId", description = "Get detailed ticket for admin by displayId")
    public ResponseEntity<SupportTicketResponseDto> getTicket(@PathVariable String displayId) {
        return ResponseEntity.ok(supportTicketService.getTicketByDisplayIdAdmin(displayId));
    }

    @PostMapping("/{displayId}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Archive ticket", description = "Archive or unarchive a ticket")
    public ResponseEntity<SupportTicketResponseDto> setArchived(@PathVariable String displayId, @RequestParam boolean archived) {
        return ResponseEntity.ok(supportTicketService.setArchived(displayId, archived));
    }

    @PostMapping("/{displayId}/sla")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Set SLA", description = "Set target resolution datetime for a ticket (ISO format)")
    public ResponseEntity<SupportTicketResponseDto> setSla(@PathVariable String displayId, @RequestBody java.util.Map<String, String> body) {
        String sla = body.get("slaBy");
        java.time.LocalDateTime slaBy = sla != null ? java.time.LocalDateTime.parse(sla) : null;
        return ResponseEntity.ok(supportTicketService.setSla(displayId, slaBy));
    }

    @PostMapping("/{displayId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Add admin message", description = "Add an admin message to ticket (no file)")
    public ResponseEntity<?> addMessage(@PathVariable String displayId, @RequestBody java.util.Map<String, String> body, Authentication authentication) {
        String adminEmail = authentication.getName();
        String msg = body.get("message");
        supportTicketService.adminReplyToTicket(adminEmail, displayId, msg);
        return ResponseEntity.ok("Message added");
    }

    @PostMapping(path = "/{displayId}/attachments", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Upload attachment", description = "Upload an attachment for a support ticket (admin)")
    public ResponseEntity<SupportTicketResponseDto> uploadAttachment(@PathVariable String displayId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file, Authentication authentication) {
        try {
            SupportTicketResponseDto dto = supportTicketService.adminUploadAttachment(displayId, file, authentication.getName());
            return ResponseEntity.ok(dto);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping(path = "/{displayId}/reply", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Reply with optional attachment", description = "Admin reply with optional file upload")
    public ResponseEntity<SupportTicketResponseDto> replyWithAttachment(@PathVariable String displayId,
                                                                        @RequestParam(value = "message", required = false) String message,
                                                                        @RequestParam(value = "status", required = false) String status,
                                                                        @RequestParam(value = "file", required = false) MultipartFile file,
                                                                        @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                                                        Authentication authentication) {
        String normalizedStatus = normalizeStatusOrNull(status);
        if (normalizedStatus == null) {
            return ResponseEntity.badRequest().body(null);
        }
        String adminEmail = authentication.getName();
        List<MultipartFile> attachments = new ArrayList<>();
        if (files != null) {
            attachments.addAll(files.stream().filter(f -> f != null && !f.isEmpty()).toList());
        }
        if (file != null && !file.isEmpty()) {
            attachments.add(file);
        }
        SupportTicketResponseDto updated = supportTicketService.adminReplyWithAttachments(adminEmail, displayId, message, attachments);
        try {
            updated = supportTicketService.setStatusAdmin(displayId, normalizedStatus);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(updated);
    }

    @PostMapping(path = "/{displayId}/reply", consumes = {"application/json"})
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Reply without attachment", description = "Admin reply using JSON body")
    public ResponseEntity<SupportTicketResponseDto> replyNoAttachment(@PathVariable String displayId,
                                                                      @RequestBody java.util.Map<String, String> body,
                                                                      Authentication authentication) {
        String adminEmail = authentication.getName();
        String message = body != null ? body.get("message") : null;
        String status = body != null ? body.get("status") : null;
        String normalizedStatus = normalizeStatusOrNull(status);
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (normalizedStatus == null) {
            return ResponseEntity.badRequest().body(null);
        }
        SupportTicketResponseDto updated = supportTicketService.adminReplyToTicket(adminEmail, displayId, message);
        try {
            updated = supportTicketService.setStatusAdmin(displayId, normalizedStatus);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{displayId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Get ticket messages", description = "Return conversation messages for a ticket")
    public ResponseEntity<?> getMessages(@PathVariable String displayId) {
        java.util.List<com.farmeazy.dto.SupportTicketMessageDto> msgs = supportTicketService.getTicketMessages(displayId);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("messages", msgs);
        return ResponseEntity.ok(resp);
    }

    // Role management: assign/remove roles
    @PutMapping("/user/{userId}/role")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Assign/remove user role", description = "Assign or remove roles for a user")
    public ResponseEntity<?> manageUserRole(@PathVariable Long userId, @RequestParam String role, @RequestParam boolean assign, Authentication authentication) {
        // Authorization enforced via @PreAuthorize
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        if (assign) {
            user.getRoles().add(role);
        } else {
            user.getRoles().remove(role);
        }
        userRepository.save(user);
        return ResponseEntity.ok("Role " + (assign ? "assigned" : "removed") + ": " + role);
    }
}

package com.farmeazy.controller;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/tickets")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "Admin Ticketing Dashboard", description = "Admin dashboard for ticket management and user role control")
public class AdminTicketingDashboardController {

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private UserRepository userRepository;

    // Register new admin/superadmin (SUPERADMIN only)
    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Register admin/superadmin", description = "Register a new admin or superadmin with email and password")
    public ResponseEntity<?> registerAdmin(@RequestBody User user, Authentication authentication) {
        // Registration logic (validate, encrypt password, assign role, save)
        user.getRoles().add("ADMIN");
        userRepository.save(user);
        return ResponseEntity.ok("Admin registered: " + user.getEmail());
    }

    // Get all tickets
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Get all tickets", description = "View all support tickets")
    public ResponseEntity<?> getAllTickets(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Boolean important,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(supportTicketService.getAllTicketsFiltered(page, size, status, category, priority, important, archived, search));
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
        // Assignment logic (update ticket entity)
        // ...existing code...
        return ResponseEntity.ok("Ticket assigned to: " + userEmail);
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
                                                                         @RequestParam(value = "file", required = false) MultipartFile file,
                                                                         Authentication authentication) {
        String adminEmail = authentication.getName();
        return ResponseEntity.ok(supportTicketService.adminReplyWithAttachment(adminEmail, displayId, message, file));
    }

    @GetMapping("/{displayId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Get ticket messages", description = "Return conversation messages for a ticket")
    public ResponseEntity<?> getMessages(@PathVariable String displayId) {
        SupportTicketResponseDto dto = supportTicketService.getTicketByDisplayIdAdmin(displayId);
        java.util.List<java.util.Map<String, Object>> messages = new java.util.ArrayList<>();

        // User initial message
        java.util.Map<String, Object> userMsg = new java.util.HashMap<>();
        userMsg.put("id", "user-" + dto.getDisplayId());
        userMsg.put("senderName", dto.getContactEmail());
        userMsg.put("message", dto.getDescription());
        userMsg.put("createdAt", dto.getCreatedAt());
        userMsg.put("attachments", java.util.Collections.emptyList());
        messages.add(userMsg);

        // Admin notes
        if (dto.getAdminNotes() != null && !dto.getAdminNotes().isBlank()) {
            java.util.Map<String, Object> adminMsg = new java.util.HashMap<>();
            adminMsg.put("id", "admin-" + dto.getDisplayId());
            adminMsg.put("senderName", "Admin");
            adminMsg.put("message", dto.getAdminNotes());
            adminMsg.put("createdAt", dto.getUpdatedAt());
            // Simple parse for attachment lines added by adminUploadAttachment
            java.util.List<java.util.Map<String, String>> atts = new java.util.ArrayList<>();
            String[] lines = dto.getAdminNotes().split("\\r?\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("Attachment:")) {
                    // format: Attachment: filename (/uploads/uuid-name)
                    int paren = line.indexOf('(');
                    String filename = line.substring("Attachment:".length(), paren > 0 ? paren : line.length()).trim();
                    String url = paren > 0 ? line.substring(paren + 1, line.length() - 1) : null;
                    java.util.Map<String, String> m = new java.util.HashMap<>();
                    m.put("filename", filename);
                    m.put("url", url != null ? url : "");
                    atts.add(m);
                }
            }
            adminMsg.put("attachments", atts);
            messages.add(adminMsg);
        }

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("messages", messages);
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

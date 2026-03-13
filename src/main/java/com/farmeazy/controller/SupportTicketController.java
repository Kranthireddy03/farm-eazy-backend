package com.farmeazy.controller;

import com.farmeazy.dto.SupportTicketDto;
import com.farmeazy.dto.SupportTicketResponseDto;
import com.farmeazy.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/support-tickets")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
@Tag(name = "Support Tickets", description = "Endpoints for users to create and manage their own support tickets")
public class SupportTicketController {
    @PostMapping("/guest")
    @Operation(summary = "Create guest ticket", description = "Create a new support ticket for a guest user (no authentication required)")
    public ResponseEntity<SupportTicketResponseDto> createGuestTicket(@Valid @RequestBody SupportTicketDto dto) {
        if (dto.getContactEmail() == null || dto.getContactEmail().isBlank()) {
            throw new com.farmeazy.exception.ResourceNotFoundException("Contact email is required for guest ticket");
        }
        SupportTicketResponseDto created = supportTicketService.createGuestTicket(dto);
        return ResponseEntity.ok(created);
    }

    @Autowired
    private SupportTicketService supportTicketService;

    @PostMapping
    @Operation(summary = "Create ticket", description = "Create a new support ticket for the authenticated user")
    public ResponseEntity<SupportTicketResponseDto> createTicket(@Valid @RequestBody SupportTicketDto dto, Authentication authentication) {
        String email = authentication.getName();
        SupportTicketResponseDto created = supportTicketService.createTicket(email, dto);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    @Operation(summary = "List tickets", description = "List tickets for the authenticated user; SUPERADMIN sees all tickets")
    public ResponseEntity<?> listTickets(Authentication authentication) {
        boolean isSuper = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
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
        if (isSuper) {
            return ResponseEntity.ok(supportTicketService.getTicketByDisplayIdAdmin(displayId));
        }
        return ResponseEntity.ok(supportTicketService.getTicketByDisplayId(authentication.getName(), displayId));
    }

    @PostMapping("/{displayId}/respond")
    @Operation(summary = "User respond", description = "Add a user response to their own ticket")
    public ResponseEntity<SupportTicketResponseDto> respondToTicket(@PathVariable String displayId, @RequestBody java.util.Map<String, String> body, Authentication authentication) {
        String resp = body.getOrDefault("response", "");
        SupportTicketResponseDto updated = supportTicketService.addResponse(authentication.getName(), displayId, resp);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{displayId}/cancel")
    @Operation(summary = "Cancel ticket", description = "Cancel a ticket owned by the authenticated user")
    public ResponseEntity<SupportTicketResponseDto> cancelTicket(@PathVariable String displayId, Authentication authentication) {
        SupportTicketResponseDto updated = supportTicketService.cancelTicket(authentication.getName(), displayId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/count/active")
    @Operation(summary = "Active ticket count", description = "Return count of active tickets for the authenticated user")
    public ResponseEntity<java.util.Map<String, Object>> activeCount(Authentication authentication) {
        long c = supportTicketService.getActiveTicketCount(authentication.getName());
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("activeTickets", c);
        return ResponseEntity.ok(m);
    }
}

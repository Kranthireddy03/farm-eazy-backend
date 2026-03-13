package com.farmeazy.controller;

import com.farmeazy.dto.SupportTicketResponseDto;
import com.farmeazy.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/support-tickets")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "SuperAdmin Support Tickets", description = "Super admin-only support ticket management")
public class SuperAdminSupportTicketController {

    @Autowired
    private SupportTicketService supportTicketService;

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMIN')")
    @Operation(summary = "Get all support tickets (SuperAdmin)", description = "View all support tickets as super admin")
    public ResponseEntity<List<SupportTicketResponseDto>> getAllTickets() {
        List<SupportTicketResponseDto> tickets = supportTicketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    // Add more endpoints as needed for super admin actions (reply, close, etc.)
}

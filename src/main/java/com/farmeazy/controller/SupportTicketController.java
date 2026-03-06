package com.farmeazy.controller;

import com.farmeazy.dto.SupportTicketDto;
import com.farmeazy.dto.SupportTicketResponseDto;
import com.farmeazy.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SUPPORT TICKET CONTROLLER
 * 
 * PURPOSE: REST API for customer support ticket management.
 * 
 * ENDPOINTS:
 * - POST /api/support-tickets: Create a new ticket
 * - GET /api/support-tickets: Get user's tickets
 * - GET /api/support-tickets/{displayId}: Get specific ticket
 * - POST /api/support-tickets/{displayId}/cancel: Cancel a ticket
 * - POST /api/support-tickets/{displayId}/respond: Add response to ticket
 */
@RestController
@RequestMapping("/api/support-tickets")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "Support Tickets", description = "Customer support ticket management")
public class SupportTicketController {

    @Autowired
    private SupportTicketService ticketService;

    /**
     * Create a new support ticket
     */
    @PostMapping
    @Operation(summary = "Create support ticket", description = "Submit a new customer support ticket")
    public ResponseEntity<SupportTicketResponseDto> createTicket(
            Authentication auth,
            @Valid @RequestBody SupportTicketDto dto) {
        String userEmail = auth.getName();
        SupportTicketResponseDto ticket = ticketService.createTicket(userEmail, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    /**
     * Get all tickets for the authenticated user
     */
    @GetMapping
    @Operation(summary = "Get user's tickets", description = "Retrieve all support tickets for the current user")
    public ResponseEntity<List<SupportTicketResponseDto>> getUserTickets(Authentication auth) {
        String userEmail = auth.getName();
        List<SupportTicketResponseDto> tickets = ticketService.getUserTickets(userEmail);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get a specific ticket by display ID
     */
    @GetMapping("/{displayId}")
    @Operation(summary = "Get ticket details", description = "Retrieve details of a specific support ticket")
    public ResponseEntity<SupportTicketResponseDto> getTicket(
            Authentication auth,
            @PathVariable String displayId) {
        String userEmail = auth.getName();
        SupportTicketResponseDto ticket = ticketService.getTicketByDisplayId(userEmail, displayId);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Cancel a ticket
     */
    @PostMapping("/{displayId}/cancel")
    @Operation(summary = "Cancel ticket", description = "Cancel an open support ticket")
    public ResponseEntity<SupportTicketResponseDto> cancelTicket(
            Authentication auth,
            @PathVariable String displayId) {
        String userEmail = auth.getName();
        SupportTicketResponseDto ticket = ticketService.cancelTicket(userEmail, displayId);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Add a response to a ticket
     */
    @PostMapping("/{displayId}/respond")
    @Operation(summary = "Add response", description = "Add a response or additional info to a ticket")
    public ResponseEntity<SupportTicketResponseDto> addResponse(
            Authentication auth,
            @PathVariable String displayId,
            @RequestBody Map<String, String> request) {
        String userEmail = auth.getName();
        String response = request.get("response");
        if (response == null || response.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        SupportTicketResponseDto ticket = ticketService.addResponse(userEmail, displayId, response);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Get count of active tickets
     */
    @GetMapping("/count/active")
    @Operation(summary = "Get active ticket count", description = "Get count of open/in-progress tickets")
    public ResponseEntity<Map<String, Long>> getActiveCount(Authentication auth) {
        String userEmail = auth.getName();
        long count = ticketService.getActiveTicketCount(userEmail);
        return ResponseEntity.ok(Map.of("activeTickets", count));
    }
}

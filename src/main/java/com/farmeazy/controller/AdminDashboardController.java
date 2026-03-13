package com.farmeazy.controller;

import com.farmeazy.entity.SupportTicket;
import com.farmeazy.entity.SupportTicket.TicketStatus;
import com.farmeazy.repository.SupportTicketRepository;
import com.farmeazy.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "Admin Dashboard", description = "Admin dashboard metrics and summaries")
public class AdminDashboardController {

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    @Operation(summary = "Get dashboard metrics")
    public ResponseEntity<?> getMetrics() {
        Map<String, Object> m = new HashMap<>();

        long open = ticketRepository.countByStatus(TicketStatus.OPEN);
        long inProgress = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long pending = ticketRepository.countByStatus(TicketStatus.PENDING_USER);
        long resolved = ticketRepository.countByStatus(TicketStatus.RESOLVED);

        m.put("tickets", Map.of(
                "open", open,
                "inProgress", inProgress,
                "pendingUser", pending,
                "resolved", resolved
        ));

        long totalUsers = userRepository.count();
        m.put("users", Map.of("total", totalUsers));

        long overdue = ticketRepository.findAll().stream().filter(t -> t.getSlaBy() != null && t.getSlaBy().isBefore(java.time.LocalDateTime.now())).count();
        m.put("sla", Map.of("overdue", overdue));

        return ResponseEntity.ok(m);
    }
}

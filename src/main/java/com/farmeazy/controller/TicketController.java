package com.farmeazy.controller;

import com.farmeazy.entity.Ticket;
import com.farmeazy.entity.TicketMessage;
import com.farmeazy.model.TicketStatus;
import com.farmeazy.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/tickets")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping
    public ResponseEntity<Ticket> createTicket(@RequestBody Ticket ticket) {
        Ticket created = ticketService.createTicket(ticket);
        return ResponseEntity.created(URI.create("/tickets/" + created.getId())).body(created);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN') or hasRole('SUPPORT')")
    public Page<Ticket> listTickets(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String priority,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) Long userId) {
        Pageable p = PageRequest.of(page, size);
        return ticketService.listTickets(p, status, priority, category, userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long id) {
        Optional<Ticket> t = ticketService.getTicket(id);
        return t.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<java.util.List<Ticket>> getUserTickets(@PathVariable Long userId) {
        java.util.List<Ticket> list = ticketService.listTicketsByUser(userId);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN') or hasRole('SUPPORT')")
    public ResponseEntity<Ticket> updateStatus(@PathVariable Long id, @RequestParam TicketStatus status) {
        Ticket updated = ticketService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<Ticket> addMessage(@PathVariable Long id, @RequestBody TicketMessage message) {
        Ticket t = ticketService.addMessage(id, message);
        return ResponseEntity.ok(t);
    }

    @PostMapping(path = "/{id}/attachments", consumes = {"multipart/form-data"})
    public ResponseEntity<Ticket> uploadAttachment(@PathVariable Long id, @RequestParam("file") org.springframework.web.multipart.MultipartFile file, @RequestParam(required = false) Long uploadedBy) {
        try {
            // Save file to local uploads directory
            java.nio.file.Path uploads = java.nio.file.Paths.get("uploads");
            if (!java.nio.file.Files.exists(uploads)) java.nio.file.Files.createDirectories(uploads);
            String filename = System.currentTimeMillis() + "-" + java.util.UUID.randomUUID() + "-" + file.getOriginalFilename();
            java.nio.file.Path target = uploads.resolve(filename);
            try (java.io.InputStream in = file.getInputStream()) {
                java.nio.file.Files.copy(in, target);
            }
            String fileUrl = "/uploads/" + filename;
            Ticket t = ticketService.addAttachment(id, fileUrl, uploadedBy == null ? 0L : uploadedBy);
            return ResponseEntity.ok(t);
        } catch (Exception ex) {
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    public ResponseEntity<Ticket> assign(@PathVariable Long id, @RequestParam Long userId) {
        Ticket t = ticketService.assign(id, userId);
        return ResponseEntity.ok(t);
    }

    @PutMapping("/{id}/important")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN') or hasRole('SUPPORT')")
    public ResponseEntity<Ticket> markImportant(@PathVariable Long id, @RequestParam boolean important) {
        Ticket t = ticketService.markImportant(id, important);
        return ResponseEntity.ok(t);
    }
}

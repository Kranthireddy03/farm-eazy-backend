package com.farmeazy.service;

import com.farmeazy.entity.Ticket;
import com.farmeazy.entity.TicketMessage;
import com.farmeazy.model.TicketPriority;
import com.farmeazy.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface TicketService {
    Ticket createTicket(Ticket ticket);
    Page<Ticket> listTickets(Pageable pageable, String status, String priority, String category, Long userId);
    Optional<Ticket> getTicket(Long id);
    Optional<Ticket> getByDisplayId(String displayId);
    Ticket addMessage(Long ticketId, TicketMessage message);
    Ticket updateStatus(Long ticketId, TicketStatus status);
    Ticket assign(Long ticketId, Long userId);
    Ticket markImportant(Long ticketId, boolean important);
    java.util.List<com.farmeazy.entity.Ticket> listTicketsByUser(Long userId);
    com.farmeazy.entity.Ticket addAttachment(Long ticketId, String fileUrl, Long uploadedBy);
}

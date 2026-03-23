package com.farmeazy.service.impl;

import com.farmeazy.entity.Ticket;
import com.farmeazy.entity.TicketMessage;
import com.farmeazy.model.TicketPriority;
import com.farmeazy.model.TicketStatus;
import com.farmeazy.repository.TicketMessageRepository;
import com.farmeazy.repository.TicketRepository;
import com.farmeazy.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final com.farmeazy.repository.TicketAttachmentRepository attachmentRepository;

    @Autowired
    public TicketServiceImpl(TicketRepository ticketRepository, TicketMessageRepository messageRepository, com.farmeazy.repository.TicketAttachmentRepository attachmentRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    @Transactional
    public Ticket createTicket(Ticket ticket) {
        if (ticket.getDisplayId() == null) {
            ticket.setDisplayId("TCK-" + System.currentTimeMillis());
        }
        if (ticket.getStatus() == null) ticket.setStatus("OPEN");
        ticket.setCreatedAt(OffsetDateTime.now());
        ticket.setUpdatedAt(OffsetDateTime.now());
        return ticketRepository.save(ticket);
    }

    @Override
    public Page<Ticket> listTickets(Pageable pageable, String status, String priority, String category, Long userId) {
        // Basic implementation: ignore filters if null. For production use, implement Specification.
        return ticketRepository.findAll(pageable);
    }

    @Override
    public java.util.List<Ticket> listTicketsByUser(Long userId) {
        return ticketRepository.findAllByCreatedBy(userId);
    }

    @Override
    public Optional<Ticket> getTicket(Long id) {
        return ticketRepository.findById(id);
    }

    @Override
    public Optional<Ticket> getByDisplayId(String displayId) {
        return ticketRepository.findByDisplayId(displayId);
    }

    @Override
    @Transactional
    public Ticket addMessage(Long ticketId, TicketMessage message) {
        message.setTicketId(ticketId);
        message.setCreatedAt(OffsetDateTime.now());
        messageRepository.save(message);
        Optional<Ticket> t = ticketRepository.findById(ticketId);
        t.ifPresent(ticket -> {
            ticket.setUpdatedAt(OffsetDateTime.now());
            ticketRepository.save(ticket);
        });
        return t.orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    @Override
    @Transactional
    public Ticket addAttachment(Long ticketId, String fileUrl, Long uploadedBy) {
        com.farmeazy.entity.TicketAttachment att = new com.farmeazy.entity.TicketAttachment();
        att.setTicketId(ticketId);
        att.setFileUrl(fileUrl);
        att.setUploadedBy(uploadedBy);
        att.setCreatedAt(OffsetDateTime.now());
        attachmentRepository.save(att);
        Optional<Ticket> t = ticketRepository.findById(ticketId);
        t.ifPresent(ticket -> {
            ticket.setUpdatedAt(OffsetDateTime.now());
            ticketRepository.save(ticket);
        });
        return t.orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    @Override
    @Transactional
    public Ticket updateStatus(Long ticketId, TicketStatus status) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status != null ? status.name() : null);
        ticket.setUpdatedAt(OffsetDateTime.now());
        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public Ticket assign(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setAssignedTo(userId);
        ticket.setStatus("ASSIGNED");
        ticket.setUpdatedAt(OffsetDateTime.now());
        return ticketRepository.save(ticket);
    }

    @Override
    @Transactional
    public Ticket markImportant(Long ticketId, boolean important) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setImportant(important);
        ticket.setUpdatedAt(OffsetDateTime.now());
        return ticketRepository.save(ticket);
    }
}

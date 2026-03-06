package com.farmeazy.service;

import com.farmeazy.dto.SupportTicketDto;
import com.farmeazy.dto.SupportTicketResponseDto;
import com.farmeazy.entity.SupportTicket;
import com.farmeazy.entity.SupportTicket.TicketStatus;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.SupportTicketRepository;
import com.farmeazy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SUPPORT TICKET SERVICE
 * 
 * PURPOSE: Manages customer support tickets.
 */
@Service
public class SupportTicketService {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketService.class);

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new support ticket
     */
    @Transactional
    public SupportTicketResponseDto createTicket(String userEmail, SupportTicketDto dto) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setSubject(dto.getSubject());
        ticket.setDescription(dto.getDescription());
        ticket.setCategory(dto.getCategory() != null ? dto.getCategory() : SupportTicket.TicketCategory.GENERAL);
        ticket.setPriority(dto.getPriority() != null ? dto.getPriority() : SupportTicket.TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setContactEmail(dto.getContactEmail() != null ? dto.getContactEmail() : user.getEmail());
        ticket.setContactPhone(dto.getContactPhone() != null ? dto.getContactPhone() : user.getPhone());
        ticket.setOrderId(dto.getOrderId());
        ticket.setServiceId(dto.getServiceId());

        // Save to get ID
        SupportTicket saved = ticketRepository.save(ticket);
        
        // Generate display ID
        saved.setDisplayId(String.format("INC%05d", saved.getId()));
        saved = ticketRepository.save(saved);

        logger.info("Created support ticket {} for user {}", saved.getDisplayId(), userEmail);

        return SupportTicketResponseDto.fromEntity(saved);
    }

    /**
     * Get all tickets for a user
     */
    @Transactional(readOnly = true)
    public List<SupportTicketResponseDto> getUserTickets(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ticketRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(SupportTicketResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific ticket by display ID
     */
    @Transactional(readOnly = true)
    public SupportTicketResponseDto getTicketByDisplayId(String userEmail, String displayId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));

        // Verify ownership
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Ticket not found: " + displayId);
        }

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * Cancel a ticket (user action)
     */
    @Transactional
    public SupportTicketResponseDto cancelTicket(String userEmail, String displayId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));

        // Verify ownership
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Ticket not found: " + displayId);
        }

        // Can only cancel OPEN or IN_PROGRESS tickets
        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot cancel ticket with status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setClosedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        logger.info("Ticket {} cancelled by user {}", displayId, userEmail);

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * Add response to a ticket (updates description)
     */
    @Transactional
    public SupportTicketResponseDto addResponse(String userEmail, String displayId, String response) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SupportTicket ticket = ticketRepository.findByDisplayId(displayId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + displayId));

        // Verify ownership
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Ticket not found: " + displayId);
        }

        // Can only add response to open tickets
        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Cannot add response to closed ticket");
        }

        // Append response to description
        String updatedDescription = ticket.getDescription() + "\n\n--- User Response (" + LocalDateTime.now() + ") ---\n" + response;
        ticket.setDescription(updatedDescription);
        
        // If pending user response, move back to open
        if (ticket.getStatus() == TicketStatus.PENDING_USER) {
            ticket.setStatus(TicketStatus.OPEN);
        }
        
        ticketRepository.save(ticket);

        logger.info("Response added to ticket {} by user {}", displayId, userEmail);

        return SupportTicketResponseDto.fromEntity(ticket);
    }

    /**
     * Get count of active tickets for a user
     */
    @Transactional(readOnly = true)
    public long getActiveTicketCount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ticketRepository.countByUserAndStatus(user, TicketStatus.OPEN) +
               ticketRepository.countByUserAndStatus(user, TicketStatus.IN_PROGRESS) +
               ticketRepository.countByUserAndStatus(user, TicketStatus.PENDING_USER);
    }
}

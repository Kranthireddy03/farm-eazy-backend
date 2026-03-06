package com.farmeazy.repository;

import com.farmeazy.entity.SupportTicket;
import com.farmeazy.entity.SupportTicket.TicketStatus;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    
    Optional<SupportTicket> findByDisplayId(String displayId);
    
    List<SupportTicket> findByUserOrderByCreatedAtDesc(User user);
    
    List<SupportTicket> findByUserAndStatusOrderByCreatedAtDesc(User user, TicketStatus status);
    
    List<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status);
    
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    
    long countByUserAndStatus(User user, TicketStatus status);
    
    long countByStatus(TicketStatus status);
}

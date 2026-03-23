package com.farmeazy.repository;

import com.farmeazy.entity.SupportTicket;
import com.farmeazy.entity.SupportTicket.TicketStatus;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {
    
    Optional<SupportTicket> findByDisplayId(String displayId);
    
    List<SupportTicket> findByUserOrderByCreatedAtDesc(User user);

    List<SupportTicket> findByContactEmailOrderByCreatedAtDesc(String contactEmail);
    
    List<SupportTicket> findByUserAndStatusOrderByCreatedAtDesc(User user, TicketStatus status);
    
    List<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status);
    
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    
    long countByUserAndStatus(User user, TicketStatus status);
    
    long countByStatus(TicketStatus status);

    @Query(value = "SELECT COUNT(*) FROM support_tickets WHERE created_at >= :start AND created_at < :end", nativeQuery = true)
    long countByDate(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}

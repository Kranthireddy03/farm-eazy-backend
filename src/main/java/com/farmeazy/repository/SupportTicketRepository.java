package com.farmeazy.repository;

import com.farmeazy.entity.SupportTicket;
import com.farmeazy.entity.SupportTicket.TicketStatus;
import com.farmeazy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    long countByAssignedToIsNullAndStatusIn(List<TicketStatus> statuses);

    long countByAssignedToIsNotNullAndStatusIn(List<TicketStatus> statuses);

    long countByAssignedToAndStatusIn(String assignedTo, List<TicketStatus> statuses);

    long countByUser(User user);

    long countByUserAndStatusIn(User user, List<TicketStatus> statuses);

    List<SupportTicket> findByAssignedToAndStatusIn(String assignedTo, List<TicketStatus> statuses);

    @Query(value = "SELECT COUNT(*) FROM support_tickets WHERE created_at >= :start AND created_at < :end", nativeQuery = true)
    long countByDate(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("SELECT MIN(t.createdAt) FROM SupportTicket t")
    LocalDateTime findEarliestCreatedAt();

    @Query("SELECT MIN(t.createdAt) FROM SupportTicket t WHERE lower(t.source) = 'public' OR (t.source IS NULL AND t.user IS NULL)")
    LocalDateTime findEarliestPublicCreatedAt();

    @Query("SELECT MIN(t.createdAt) FROM SupportTicket t WHERE lower(t.source) = 'login' OR t.user IS NOT NULL")
    LocalDateTime findEarliestLoginCreatedAt();
}

package com.farmeazy.repository;

import com.farmeazy.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.time.OffsetDateTime;
import java.time.LocalDate;
// import com.farmeazy.model.TicketStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    Optional<Ticket> findByDisplayId(String displayId);
    List<Ticket> findAllByCreatedBy(Long createdBy);

    long countByStatus(String status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'RESOLVED' AND t.updatedAt >= :start")
    long countResolvedSince(@Param("start") OffsetDateTime start);

    List<Ticket> findTop10ByCreatedAtAfterOrderByCreatedAtDesc(OffsetDateTime start);

    List<Ticket> findTop10ByStatusNotInOrderByCreatedAtDesc(List<String> statuses);
    List<Ticket> findTop10ByCreatedAtAfterAndStatusNotInOrderByCreatedAtDesc(OffsetDateTime start, List<String> statuses);

    @Query(value = "SELECT COUNT(*) FROM support_tickets WHERE created_at >= :start AND created_at < :end", nativeQuery = true)
    long countByDate(@Param("start") java.time.OffsetDateTime start, @Param("end") java.time.OffsetDateTime end);

    @Query("SELECT MIN(t.createdAt) FROM Ticket t")
    OffsetDateTime findEarliestCreatedAt();

    @Query("SELECT MIN(t.createdAt) FROM Ticket t WHERE t.createdBy IS NULL OR t.createdBy = 0")
    OffsetDateTime findEarliestPublicCreatedAt();

    @Query("SELECT MIN(t.createdAt) FROM Ticket t WHERE t.createdBy IS NOT NULL AND t.createdBy <> 0")
    OffsetDateTime findEarliestLoginCreatedAt();
    long countByStatusIgnoreCaseAndCreatedAtBetween(String status, OffsetDateTime start, OffsetDateTime end);
}

package com.farmeazy.repository;

import com.farmeazy.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    Optional<Ticket> findByDisplayId(String displayId);
    java.util.List<Ticket> findAllByCreatedBy(Long createdBy);
}

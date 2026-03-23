package com.farmeazy.repository;

import com.farmeazy.entity.SupportTicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, Long> {
    List<SupportTicketMessage> findBySupportTicketIdOrderByCreatedAtAsc(Long supportTicketId);

    List<SupportTicketMessage> findBySupportTicketIdInOrderByCreatedAtAsc(List<Long> supportTicketIds);

    boolean existsByAttachmentUrl(String attachmentUrl);

    boolean existsByMessageContaining(String text);
}

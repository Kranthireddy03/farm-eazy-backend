package com.farmeazy.repository;

import com.farmeazy.entity.SupportTicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportTicketAttachmentRepository extends JpaRepository<SupportTicketAttachment, Long> {
    List<SupportTicketAttachment> findBySupportTicketMessageId(Long messageId);
    SupportTicketAttachment findByUrl(String url);
}

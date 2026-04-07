package com.farmeazy.repository;

import com.farmeazy.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByTemplateName(String templateName);
    Optional<NotificationTemplate> findByNotificationTypeAndIsActiveTrue(String notificationType);
    List<NotificationTemplate> findByIsActiveTrue();
}

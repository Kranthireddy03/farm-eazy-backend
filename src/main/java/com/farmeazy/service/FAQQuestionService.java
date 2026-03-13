package com.farmeazy.service;
import com.farmeazy.entity.FAQCommunication;
import com.farmeazy.repository.FAQCommunicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.farmeazy.dto.FAQQuestionDto;
import com.farmeazy.entity.FAQQuestion;
import com.farmeazy.repository.FAQQuestionRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FAQQuestionService {
        @Autowired
        private HttpEmailService httpEmailService;

        @Autowired
        private FAQCommunicationRepository faqCommunicationRepository;
    // ...existing code...

    public List<FAQQuestionDto> getAllApprovedFaqs() {
        List<FAQQuestion> faqs = faqQuestionRepository.findByAddedToFAQTrue();
        return faqs.stream().map(this::toDto).toList();
    }

    public List<FAQQuestion> getUnansweredNotifications() {
        try {
            return faqQuestionRepository.findByAnswerIsNullAndNotificationReadFalse();
        } catch (org.springframework.dao.DataAccessException ex) {
            // likely DB schema doesn't have notificationRead column yet — fallback to unanswered-only
            return faqQuestionRepository.findByAnswerIsNull();
        }
    }

    private FAQQuestionDto toDto(FAQQuestion entity) {
        FAQQuestionDto dto = new FAQQuestionDto();
        dto.setId(entity.getId());
        dto.setQuestion(entity.getQuestion());
        dto.setEmail(entity.getEmail());
        dto.setUserId(entity.getUserId());
        dto.setAnswer(entity.getAnswer());
        dto.setSubmittedAt(entity.getSubmittedAt());
        // Optionally add answer and id if needed in DTO
        return dto;
    }
        public List<FAQQuestion> getAllQuestions() {
            return faqQuestionRepository.findAll();
        }
    @Autowired
    private FAQQuestionRepository faqQuestionRepository;

    @Autowired
    private NotificationSseService notificationSseService;

    @Transactional
    public void processQuestion(FAQQuestionDto dto) {
        FAQQuestion entity = new FAQQuestion();
        entity.setQuestion(dto.getQuestion());
        entity.setEmail(dto.getEmail());
        entity.setUserId(dto.getUserId());
        entity.setSubmittedAt(LocalDateTime.now());
        faqQuestionRepository.save(entity);

        // broadcast new notifications to SSE clients
        try {
            notificationSseService.sendNotifications(getUnansweredNotifications());
        } catch (Exception ignored) {}

        // Admin email notification removed. Only store question.
        // Optionally, notify user after submission (handled in controller/frontend).
    // ...existing code...
    }

    @Transactional
    public void answerQuestion(Long id, String answer, boolean addToFAQ) {
        FAQQuestion entity = faqQuestionRepository.findById(id).orElseThrow();
        entity.setAnswer(answer);
        entity.setAnsweredAt(LocalDateTime.now());
        entity.setAddedToFAQ(addToFAQ);
        faqQuestionRepository.save(entity);
        String subject;
        String body;
        String faqLink = addToFAQ ? "http://localhost:3000/support#faq" : "";
        String logo = "<img src='http://localhost:3000/farm-eazy.png' alt='FarmEazy Logo' style='height:40px;margin-bottom:16px;'/>";
        String style = "font-family:Arial,sans-serif;background:#f9fafb;padding:24px;border-radius:8px;color:#222;";
        if (addToFAQ) {
            subject = "🎉 Your Question Is Now Featured in FarmEazy FAQ!";
            body = "<div style='" + style + "'>" + logo +
                "<h2 style='color:#2563eb;'>Congratulations!</h2>" +
                "<p>Dear " + (entity.getUserId() != null ? "FarmEazy User" : "Guest") + ",</p>" +
                "<p>We appreciate your thoughtful question. It has been reviewed by our admin team and is now featured in our FAQ section to help the entire community.</p>" +
                "<div style='background:#eef2ff;padding:16px;border-radius:6px;margin:16px 0;'>" +
                "<b>Question:</b><br/>" + entity.getQuestion() + "<br/><br/>" +
                "<b>Answer:</b><br/>" + answer + "</div>" +
                "<p>You can view your question and answer <a href='" + faqLink + "' style='color:#2563eb;text-decoration:underline;'>here</a>.</p>" +
                "<p>If you have further queries or need more assistance, please <a href='http://localhost:3000/support/ticket' style='color:#059669;text-decoration:underline;'>raise a support ticket</a>.</p>" +
                "<p style='margin-top:24px;font-size:14px;color:#666;'>Thank you for contributing to FarmEazy!</p>" +
                "<hr style='margin:24px 0;border:none;border-top:1px solid #ddd;'/>" +
                "<p style='font-size:13px;color:#888;'>Best regards,<br/>FarmEazy Support Team</p></div>";
            storeCommunication(entity, subject, body, "FAQ Addition Notification");
            try {
                httpEmailService.sendEmail(entity.getEmail(), subject, body);
            } catch (Exception e) {
                System.err.println("Failed to send FAQ addition email to user: " + e.getMessage());
            }
        } else {
            subject = "✅ Response to Your FarmEazy Question";
            body = "<div style='" + style + "'>" + logo +
                "<h2 style='color:#059669;'>Your Question Answered</h2>" +
                "<p>Dear " + (entity.getUserId() != null ? "FarmEazy User" : "Guest") + ",</p>" +
                "<p>Thank you for reaching out to FarmEazy. Our admin team has reviewed your question and provided the answer below.</p>" +
                "<div style='background:#f0fdf4;padding:16px;border-radius:6px;margin:16px 0;'>" +
                "<b>Question:</b><br/>" + entity.getQuestion() + "<br/><br/>" +
                "<b>Answer:</b><br/>" + answer + "</div>" +
                "<p>If you have further queries or need more assistance, please <a href='http://localhost:3000/support/ticket' style='color:#2563eb;text-decoration:underline;'>raise a support ticket</a>.</p>" +
                "<p style='margin-top:24px;font-size:14px;color:#666;'>We are here to help you succeed!</p>" +
                "<hr style='margin:24px 0;border:none;border-top:1px solid #ddd;'/>" +
                "<p style='font-size:13px;color:#888;'>Best regards,<br/>FarmEazy Support Team</p></div>";
            storeCommunication(entity, subject, body, "Answer Notification");
            try {
                httpEmailService.sendEmail(entity.getEmail(), subject, body);
            } catch (Exception e) {
                System.err.println("Failed to send answer email to user: " + e.getMessage());
            }

            // broadcast updated notifications
            try {
                notificationSseService.sendNotifications(getUnansweredNotifications());
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    public void deleteQuestion(Long id) {
        // Remove any communications linked to this question to satisfy FK constraints
        faqCommunicationRepository.deleteByFaqQuestionId(id);
        faqQuestionRepository.deleteById(id);
        try {
            notificationSseService.sendNotifications(getUnansweredNotifications());
        } catch (Exception ignored) {}
    }

    @Transactional
    public void recordAdminCancel(Long id, String adminEmail) {
        FAQQuestion entity = faqQuestionRepository.findById(id).orElseThrow();
        String subject = "Admin cancelled FAQ review for question id: " + id;
        String body = "Question: " + entity.getQuestion() + "\nCancelled by: " + adminEmail + " on " + LocalDateTime.now();
        FAQCommunication comm = new FAQCommunication();
        comm.setFaqQuestion(entity);
        comm.setRecipientEmail(entity.getEmail());
        comm.setSubject(subject);
        comm.setBody(body);
        comm.setPurpose("Admin Cancelled Review");
        comm.setSentAt(LocalDateTime.now());
        faqCommunicationRepository.save(comm);
    }

    @Transactional
    public void markNotificationRead(Long id) {
        FAQQuestion entity = faqQuestionRepository.findById(id).orElseThrow();
        entity.setNotificationRead(true);
        faqQuestionRepository.save(entity);
        try {
            notificationSseService.sendNotifications(getUnansweredNotifications());
        } catch (Exception ignored) {}
    }

    private void storeCommunication(FAQQuestion entity, String subject, String body, String purpose) {
        FAQCommunication comm = new FAQCommunication();
        comm.setFaqQuestion(entity);
        comm.setRecipientEmail(entity.getEmail());
        comm.setSubject(subject);
        comm.setBody(body);
        comm.setPurpose(purpose);
        comm.setSentAt(LocalDateTime.now());
        faqCommunicationRepository.save(comm);
    }
}
    

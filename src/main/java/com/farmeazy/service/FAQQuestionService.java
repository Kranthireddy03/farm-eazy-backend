package com.farmeazy.service;
import com.farmeazy.entity.Notification.NotificationPriority;
import com.farmeazy.entity.Notification.NotificationType;
import com.farmeazy.entity.User;
import com.farmeazy.entity.FAQCommunication;
import com.farmeazy.repository.FAQCommunicationRepository;
import com.farmeazy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.farmeazy.dto.FAQQuestionDto;
import com.farmeazy.entity.FAQQuestion;
import com.farmeazy.repository.FAQQuestionRepository;
import com.farmeazy.entity.FAQCommunication;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.util.List;

@Service
public class FAQQuestionService {
    private static final Duration FAQ_AUTO_RESOLVE_AFTER = Duration.ofDays(3);

    @Autowired
    private HttpEmailService httpEmailService;

    @Autowired
    private FAQCommunicationRepository faqCommunicationRepository;

    @Autowired
    private FAQQuestionRepository faqQuestionRepository;

    @Autowired
    private NotificationSseService notificationSseService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${farmeazy.app.support-base-url:${FARMEAZY_SUPPORT_BASE_URL:https://support.farm-eazy.com}}")
    private String supportFrontendBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${farmeazy.app.public-base-url:${FARMEAZY_PUBLIC_BASE_URL:https://www.farm-easy.com}}")
    private String publicFrontendBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${farmeazy.app.base-url:${farmeazy.app.public-base-url:${FARMEAZY_PUBLIC_BASE_URL:https://www.farm-easy.com}}}")
    private String fallbackFrontendBaseUrl;

    public List<FAQQuestionDto> getAllApprovedFaqs() {
        List<FAQQuestion> faqs = faqQuestionRepository.findByAddedToFAQTrue();
        return faqs.stream().map(this::toDto).toList();
    }

    public List<FAQQuestion> getUnansweredNotifications() {
        try {
            return faqQuestionRepository.findByNotificationReadFalse();
        } catch (org.springframework.dao.DataAccessException ex) {
            return faqQuestionRepository.findAll();
        }
    }

    private FAQQuestionDto toDto(FAQQuestion entity) {
        FAQQuestionDto dto = new FAQQuestionDto();
        dto.setId(entity.getId());
        dto.setQuestion(entity.getQuestion());
        dto.setEmail(entity.getEmail());
        dto.setUserId(entity.getUserId());
        dto.setAnswer(entity.getAnswer());
        dto.setAddedToFAQ(entity.isAddedToFAQ());
        dto.setAnsweredAt(entity.getAnsweredAt());
        dto.setSubmittedAt(entity.getSubmittedAt());
        dto.setSource(entity.getSource());
        dto.setNotificationRead(entity.isNotificationRead());
        dto.setWorkflowStatus(calculateWorkflowStatus(entity));
        return dto;
    }

    private OffsetDateTime latestCommunicationAt(Long questionId, java.util.function.Predicate<String> purposeMatcher) {
        List<FAQCommunication> communications = faqCommunicationRepository.findByFaqQuestionIdOrderBySentAtAsc(questionId);
        OffsetDateTime latest = null;
        for (FAQCommunication comm : communications) {
            String purpose = comm.getPurpose();
            if (!purposeMatcher.test(purpose)) continue;
            OffsetDateTime sentAt = comm.getSentAt();
            if (sentAt == null) continue;
            if (latest == null || sentAt.isAfter(latest)) latest = sentAt;
        }
        return latest;
    }

    private String calculateWorkflowStatus(FAQQuestion entity) {
        if (entity == null || entity.getId() == null) return "PENDING";
        if (entity.getAnswer() == null || entity.getAnswer().isBlank()) return "PENDING";

        OffsetDateTime latestAdminReplyAt = latestCommunicationAt(entity.getId(), purpose ->
                purpose != null && (purpose.startsWith("Admin Reply") || "Answer Notification".equalsIgnoreCase(purpose) || "FAQ Addition Notification".equalsIgnoreCase(purpose))
        );
        if (latestAdminReplyAt == null) {
            latestAdminReplyAt = entity.getAnsweredAt();
        }

        OffsetDateTime latestUserEscalationAt = latestCommunicationAt(entity.getId(), purpose ->
                purpose != null && ("Sub-question".equalsIgnoreCase(purpose) || "FAQ Feedback".equalsIgnoreCase(purpose))
        );

        OffsetDateTime latestUserSolvedAt = latestCommunicationAt(entity.getId(), purpose ->
                purpose != null && "FAQ Marked Solved".equalsIgnoreCase(purpose)
        );

        if (latestUserSolvedAt != null && (latestAdminReplyAt == null || latestUserSolvedAt.isAfter(latestAdminReplyAt))) {
            return entity.isAddedToFAQ() ? "RESOLVED" : "REJECTED";
        }

        if (latestUserEscalationAt != null && (latestAdminReplyAt == null || latestUserEscalationAt.isAfter(latestAdminReplyAt))) {
            return "PENDING";
        }

        if (latestAdminReplyAt != null) {
            OffsetDateTime now = OffsetDateTime.now();
            if (Duration.between(latestAdminReplyAt, now).compareTo(FAQ_AUTO_RESOLVE_AFTER) >= 0) {
                return entity.isAddedToFAQ() ? "RESOLVED" : "REJECTED";
            }
        }

        // Awaiting explicit user confirmation or timeout.
        return "PENDING";
    }

    private String normalizeUrl(String url, String defaultUrl) {
        String out = (url != null && !url.isBlank()) ? url : defaultUrl;
        if (out == null || out.isBlank()) {
            out = "https://www.farm-easy.com";
        }
        return out.replaceAll("/$", "");
    }

    private String buildFaqUrl(Long id) {
        String baseUrl = normalizeUrl(publicFrontendBaseUrl, fallbackFrontendBaseUrl);
        return baseUrl + "/faq/" + id;
    }

    public List<FAQQuestionDto> getQuestionsForUser(String email, String userId) {
        List<FAQQuestion> rows;
        if (userId != null && !userId.isBlank() && email != null && !email.isBlank()) {
            rows = faqQuestionRepository.findByEmailOrUserIdOrderBySubmittedAtDesc(email, userId);
        } else if (userId != null && !userId.isBlank()) {
            rows = faqQuestionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        } else if (email != null && !email.isBlank()) {
            rows = faqQuestionRepository.findByEmailOrderBySubmittedAtDesc(email);
        } else {
            rows = List.of();
        }
        return rows.stream().map(this::toDto).toList();
    }

    private void notifyFaqUser(FAQQuestion question, String title, String message, NotificationPriority priority) {
        if (question == null) {
            return;
        }
        User targetUser = null;
        if (question.getUserId() != null && !question.getUserId().isBlank()) {
            try {
                Long uid = Long.parseLong(question.getUserId());
                targetUser = userRepository.findById(uid).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (targetUser == null && question.getEmail() != null && !question.getEmail().isBlank()) {
            targetUser = userRepository.findByEmail(question.getEmail()).orElse(null);
        }
        if (targetUser == null) {
            return;
        }
        notificationService.createForUser(
                targetUser,
                NotificationType.SYSTEM,
                title,
                message,
            question.getId() != null ? "/user/faq/" + question.getId() : "/user/faq",
                priority
        );
    }

    public FAQQuestionDto getPublicFaqById(Long id) {
        FAQQuestion entity = faqQuestionRepository.findById(id)
                .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("FAQ question not found: " + id));

        FAQQuestionDto dto = toDto(entity);
        dto.setCommunications(faqCommunicationRepository.findByFaqQuestionIdOrderBySentAtAsc(id)
                .stream()
                .map(comm -> {
                    com.farmeazy.dto.FAQCommunicationDto cd = new com.farmeazy.dto.FAQCommunicationDto();
                    cd.setId(comm.getId());
                    cd.setRecipientEmail(comm.getRecipientEmail());
                    cd.setSubject(comm.getSubject());
                    cd.setBody(comm.getBody());
                    cd.setPurpose(comm.getPurpose());
                    cd.setSentAt(comm.getSentAt());
                    cd.setAttachmentUrl(extractAttachmentUrlFromBody(comm.getBody()));
                    return cd;
                })
                .toList());
        return dto;
    }

    public List<FAQQuestionDto> getAllQuestionsForAdmin() {
        return faqQuestionRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public FAQQuestionDto submitFaqFeedback(Long id, Boolean satisfied, String feedback, String senderEmail) {
        FAQQuestion entity = faqQuestionRepository.findById(id)
                .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("FAQ question not found: " + id));

        if (Boolean.TRUE.equals(satisfied)) {
            entity.setAnsweredAt(OffsetDateTime.now());
            entity.setNotificationRead(false);
            faqQuestionRepository.save(entity);
            notificationSseService.sendNotifications(getUnansweredNotifications());
        } else {
            // Reopen the FAQ thread so it returns to admin pending queue for a fresh answer cycle.
            entity.setAnswer(null);
            entity.setAddedToFAQ(false);
            entity.setAnsweredAt(null);
            entity.setSubmittedAt(OffsetDateTime.now());
            entity.setNotificationRead(false);
            faqQuestionRepository.save(entity);
        }

        FAQCommunication comm = new FAQCommunication();
        comm.setFaqQuestion(entity);
        comm.setRecipientEmail(entity.getEmail());
        comm.setSubject("Feedback for your FAQ question");
        comm.setPurpose("FAQ Feedback");
        String feedbackText = (feedback != null && !feedback.isBlank()) ? feedback : "No details provided";
        comm.setBody("User " + (senderEmail != null ? senderEmail : "anonymous") + " replied: " + feedbackText + ". Satisfied: " + satisfied);
        comm.setSentAt(OffsetDateTime.now());
        faqCommunicationRepository.save(comm);

        // optionally alert support team
        try {
            String adminSubject = "FAQ feedback received (#" + id + ")";
            String adminBody = "Question: " + entity.getQuestion() + "\n" +
                    "User: " + (senderEmail != null ? senderEmail : entity.getEmail()) + "\n" +
                    "Satisfied: " + satisfied + "\n" +
                    "Feedback: " + feedbackText + "\n" +
                    "View: " + buildFaqUrl(id);
            httpEmailService.sendEmail("support@farm-eazy.com", adminSubject, adminBody);
        } catch (Exception e) {
            System.err.println("Failed to send FAQ feedback notification: " + e.getMessage());
        }

        // Notify SSE subscribers (admin UI) about feedback event
        try {
            notificationSseService.sendNotifications(getUnansweredNotifications());
        } catch (Exception ignored) {}

        return getPublicFaqById(id);
    }

    public FAQQuestion getQuestionById(Long id) {
        return faqQuestionRepository.findById(id).orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("FAQ question not found"));
    }

    public List<FAQQuestion> getAllQuestions() {
        return faqQuestionRepository.findAll();
    }

    @Transactional
    public void processQuestion(FAQQuestionDto dto) {
        FAQQuestion entity = new FAQQuestion();
        entity.setQuestion(dto.getQuestion());
        entity.setEmail(dto.getEmail());
        entity.setUserId(dto.getUserId());
        entity.setSubmittedAt(OffsetDateTime.now());

        if (dto.getUserId() != null && !dto.getUserId().isBlank()) {
            entity.setSource("FAQ_APP");
        } else if (dto.getSource() != null && !dto.getSource().isBlank()) {
            entity.setSource(dto.getSource());
        } else {
            entity.setSource("FAQ_PUBLIC_PAGE");
        }

        faqQuestionRepository.save(entity);

        notifyFaqUser(
            entity,
            "FAQ question submitted",
            "Your question was received and is pending review.",
            NotificationPriority.NORMAL
        );

        // Send user email notification on submission
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String subject = "Your FarmEazy Question Has Been Received!";
            String logo = "<img src='https://farm-eazy.com/assets/logo.png' alt='FarmEazy Logo' style='height:40px;margin-bottom:16px;'/>";
            String style = "font-family:Arial,sans-serif;background:#f9fafb;padding:24px;border-radius:8px;color:#222;";
            String body = "<div style='" + style + "'>" + logo +
                "<h2 style='color:#059669;'>Thank You for Your Question!</h2>" +
                "<p>Dear " + (entity.getUserId() != null ? "FarmEazy User" : "Guest") + ",</p>" +
                "<p>Your question has been submitted to our admin team. We will review it and reply as soon as possible.</p>" +
                "<div style='background:#eef2ff;padding:16px;border-radius:6px;margin:16px 0;'>" +
                "<b>Question:</b><br/>" + entity.getQuestion() + "</div>" +
                "<p>If your question is valuable, it may be featured in our FAQ section and you'll be notified.</p>" +
                "<p style='margin-top:24px;font-size:14px;color:#666;'>Thank you for contributing to FarmEazy!</p>" +
                "<hr style='margin:24px 0;border:none;border-top:1px solid #ddd;'/>" +
                "<p style='font-size:13px;color:#888;'>Best regards,<br/>FarmEazy Support Team</p></div>";
            try {
                httpEmailService.sendEmail(entity.getEmail(), subject, body);
            } catch (Exception e) {
                System.err.println("Failed to send FAQ submission email to user: " + e.getMessage());
            }
        }

        // broadcast new notifications to SSE clients
        try {
            notificationSseService.sendNotifications(getUnansweredNotifications());
        } catch (Exception ignored) {}
    }

    @Transactional
    public void answerQuestion(Long id, String answer, boolean addToFAQ) {
        FAQQuestion entity = faqQuestionRepository.findById(id).orElseThrow();
        String answerContext = resolvePendingAnswerContext(entity.getId());
        String answerContextLabel = toAnswerContextLabel(answerContext);
        entity.setAnswer(answer);
        entity.setAnsweredAt(OffsetDateTime.now());
        entity.setAddedToFAQ(addToFAQ);
        faqQuestionRepository.save(entity);

        notifyFaqUser(
            entity,
            addToFAQ ? "FAQ published with your question" : "FAQ answer received",
            addToFAQ
                ? "Your question has been added to the FAQ with an approved answer."
                : "Support has answered your question.",
            NotificationPriority.HIGH
        );
        String subject;
        String body;
        String recipientEmail = resolveRecipientEmail(entity);
        String baseFaqUrl = normalizeUrl(publicFrontendBaseUrl, fallbackFrontendBaseUrl);
        String supportUrl = normalizeUrl(supportFrontendBaseUrl, fallbackFrontendBaseUrl);
        String faqLink = baseFaqUrl + "/faq/" + id;
        String logo = "<img src='" + baseFaqUrl + "/assets/logo.png' alt='FarmEazy Logo' style='height:40px;margin-bottom:16px;'/>";
        String style = "font-family:Arial,sans-serif;background:#f9fafb;padding:24px;border-radius:8px;color:#222;";
        if (addToFAQ) {
            subject = "🎉 Your Question Is Now Featured in FarmEazy FAQ!";
            body = "<div style='" + style + "'>" + logo +
                "<h2 style='color:#2563eb;'>Congratulations!</h2>" +
                "<p>Dear " + (entity.getUserId() != null ? "FarmEazy User" : "Guest") + ",</p>" +
                "<p>We appreciate your thoughtful question. It has been reviewed by our admin team and is now featured in our FAQ section to help the entire community.</p>" +
                "<div style='background:#eef2ff;padding:16px;border-radius:6px;margin:16px 0;'>" +
                "<b>Response Context:</b><br/>" + answerContextLabel + "<br/><br/>" +
                "<b>Question:</b><br/>" + entity.getQuestion() + "<br/><br/>" +
                "<b>Answer:</b><br/>" + answer + "</div>" +
                "<p>You can view your question and answer <a href='" + faqLink + "' style='color:#2563eb;text-decoration:underline;'>here</a>.</p>" +
                "<p>If you have further queries or need more assistance, please <a href='" + supportUrl + "/support' style='color:#059669;text-decoration:underline;'>raise a support ticket</a>.</p>" +
                "<p style='margin-top:24px;font-size:14px;color:#666;'>Thank you for contributing to FarmEazy!</p>" +
                "<hr style='margin:24px 0;border:none;border-top:1px solid #ddd;'/>" +
                "<p style='font-size:13px;color:#888;'>Best regards,<br/>FarmEazy Support Team</p></div>";
            storeCommunication(entity, subject, body, "Admin Reply - " + answerContextLabel + " (Published)");
            try {
                httpEmailService.sendEmail(recipientEmail, subject, body);
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
                "<b>Response Context:</b><br/>" + answerContextLabel + "<br/><br/>" +
                "<b>Question:</b><br/>" + entity.getQuestion() + "<br/><br/>" +
                "<b>Answer:</b><br/>" + answer + "</div>" +
                "<p>If you have further queries or need more assistance, please <a href='" + supportUrl + "/support' style='color:#2563eb;text-decoration:underline;'>raise a support ticket</a>.</p>" +
                "<p style='margin-top:24px;font-size:14px;color:#666;'>We are here to help you succeed!</p>" +
                "<hr style='margin:24px 0;border:none;border-top:1px solid #ddd;'/>" +
                "<p style='font-size:13px;color:#888;'>Best regards,<br/>FarmEazy Support Team</p></div>";
            storeCommunication(entity, subject, body, "Admin Reply - " + answerContextLabel);
            try {
                httpEmailService.sendEmail(recipientEmail, subject, body);
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
    public FAQQuestionDto reopenQuestion(Long id, String requester, String userSubQuestion) {
        FAQQuestion entity = faqQuestionRepository.findById(id)
                .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("FAQ question not found: " + id));

        // Treat as a fresh review cycle while keeping original data
        entity.setAnswer(null);
        entity.setAddedToFAQ(false);
        entity.setAnsweredAt(null);
        entity.setSubmittedAt(java.time.OffsetDateTime.now());
        if (entity.getSource() == null || entity.getSource().isBlank()) {
            entity.setSource(requester != null && !requester.isBlank() ? "FAQ_USER_APP" : "FAQ_PUBLIC_PAGE");
        }
        faqQuestionRepository.save(entity);

        notifyFaqUser(
            entity,
            "FAQ reopened",
            "Your FAQ thread has been reopened for additional follow-up.",
            NotificationPriority.NORMAL
        );

        entity.setNotificationRead(false);
        faqQuestionRepository.save(entity);

        // Add reopen history event
        FAQCommunication reopenComm = new FAQCommunication();
        reopenComm.setFaqQuestion(entity);
        reopenComm.setRecipientEmail(entity.getEmail());
        reopenComm.setSubject("FAQ reopened by " + (requester != null && !requester.isBlank() ? requester : "Guest"));
        reopenComm.setPurpose("FAQ reopened");
        reopenComm.setBody("FAQ has been reopened for additional input.");
        reopenComm.setSentAt(java.time.OffsetDateTime.now());
        faqCommunicationRepository.save(reopenComm);

        // Add user-sub-question if provided
        if (userSubQuestion != null && !userSubQuestion.isBlank()) {
            FAQCommunication subComm = new FAQCommunication();
            subComm.setFaqQuestion(entity);
            subComm.setRecipientEmail(entity.getEmail());
            subComm.setSubject("Follow-up question from " + (requester != null && !requester.isBlank() ? requester : "Guest"));
            subComm.setPurpose("Sub-question");
            subComm.setBody(userSubQuestion);
            subComm.setSentAt(java.time.OffsetDateTime.now());
            faqCommunicationRepository.save(subComm);

            // notify support team by email
            try {
                String adminSubject = "New sub-question for FAQ #" + id;
                String adminBody = "Question: " + entity.getQuestion() + "\n" +
                        "Follow-up: " + userSubQuestion + "\n" +
                        "User: " + (requester != null && !requester.isBlank() ? requester : "Guest") + "\n" +
                        "View: " + buildFaqUrl(id);
                httpEmailService.sendEmail("support@farm-eazy.com", adminSubject, adminBody);
            } catch (Exception e) {
                System.err.println("Failed to send sub-question notification to admin: " + e.getMessage());
            }
        }

        // Notify admin/UI with SSE bell update
        try {
            notificationSseService.sendNotifications(getUnansweredNotifications());
        } catch (Exception ignored) {}

        return getPublicFaqById(id);
    }

    private String resolvePendingAnswerContext(Long faqQuestionId) {
        List<FAQCommunication> communications = faqCommunicationRepository.findByFaqQuestionIdOrderBySentAtAsc(faqQuestionId);
        for (int i = communications.size() - 1; i >= 0; i--) {
            String purpose = communications.get(i).getPurpose();
            if (purpose == null) continue;
            if ("Sub-question".equalsIgnoreCase(purpose)) return "FOLLOW_UP_QUESTION";
            if ("FAQ Feedback".equalsIgnoreCase(purpose)) return "UNCLEAR_FEEDBACK";
            if (purpose.startsWith("Admin Reply")) {
                // Last admin reply reached; no newer user escalation found.
                return "MAIN_QUESTION";
            }
        }
        return "MAIN_QUESTION";
    }

    private String toAnswerContextLabel(String context) {
        if ("FOLLOW_UP_QUESTION".equals(context)) return "Follow-up question";
        if ("UNCLEAR_FEEDBACK".equals(context)) return "Unclear feedback";
        return "Main question";
    }

    @Transactional
    public FAQQuestionDto markQuestionSolved(Long id, String resolver) {
        FAQQuestion entity = faqQuestionRepository.findById(id)
                .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("FAQ question not found: " + id));

        entity.setAddedToFAQ(true);
        entity.setNotificationRead(false);
        if (entity.getAnsweredAt() == null) {
            entity.setAnsweredAt(java.time.OffsetDateTime.now());
        }
        faqQuestionRepository.save(entity);

        storeCommunication(entity,
                "FAQ marked solved",
                "This question is marked solved by " + (resolver != null ? resolver : "an agent") + ".",
                "FAQ Marked Solved");

        try {
            notificationSseService.sendNotifications(getUnansweredNotifications());
        } catch (Exception ignored) {}
        return getPublicFaqById(id);
    }

    @Transactional
    public FAQQuestionDto addInternalNote(Long id, String note, String author) {
        FAQQuestion entity = faqQuestionRepository.findById(id)
                .orElseThrow(() -> new com.farmeazy.exception.ResourceNotFoundException("FAQ question not found: " + id));

        FAQCommunication comm = new FAQCommunication();
        comm.setFaqQuestion(entity);
        comm.setRecipientEmail(entity.getEmail());
        comm.setSubject("Internal note by " + (author != null ? author : "admin"));
        comm.setPurpose("Internal note");
        comm.setBody(note != null && !note.isBlank() ? note : "No details provided");
        comm.setSentAt(java.time.OffsetDateTime.now());
        faqCommunicationRepository.save(comm);

        return getPublicFaqById(id);
    }

    @Transactional
    public void recordAdminCancel(Long id, String adminEmail) {
        FAQQuestion entity = faqQuestionRepository.findById(id).orElseThrow();
        String subject = "Admin cancelled FAQ review for question id: " + id;
        String body = "Question: " + entity.getQuestion() + "\nCancelled by: " + adminEmail + " on " + OffsetDateTime.now();
        FAQCommunication comm = new FAQCommunication();
        comm.setFaqQuestion(entity);
        comm.setRecipientEmail(entity.getEmail());
        comm.setSubject(subject);
        comm.setBody(body);
        comm.setPurpose("Admin Cancelled Review");
        comm.setSentAt(OffsetDateTime.now());
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

    private String htmlToPlainText(String value) {
        if (value == null) return "";
        String plain = value
                .replaceAll("(?i)</p>|</div>|</h[1-6]>", "\n\n")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<li>", "- ")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
        return plain;
    }

    private String resolveRecipientEmail(FAQQuestion entity) {
        if (entity == null) return "support@farm-eazy.com";
        if (entity.getEmail() != null && !entity.getEmail().isBlank()) {
            return entity.getEmail();
        }
        if (entity.getUserId() != null && !entity.getUserId().isBlank()) {
            try {
                Long uid = Long.parseLong(entity.getUserId());
                User user = userRepository.findById(uid).orElse(null);
                if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                    return user.getEmail();
                }
            } catch (NumberFormatException ignored) {}
        }
        return "support@farm-eazy.com";
    }

    private void storeCommunication(FAQQuestion entity, String subject, String body, String purpose) {
        FAQCommunication comm = new FAQCommunication();
        comm.setFaqQuestion(entity);
        comm.setRecipientEmail(resolveRecipientEmail(entity));
        comm.setSubject(subject);
        comm.setBody(htmlToPlainText(body));
        comm.setPurpose(purpose);
        comm.setSentAt(OffsetDateTime.now());
        faqCommunicationRepository.save(comm);
    }

    private String extractAttachmentUrlFromBody(String body) {
        if (body == null || body.isBlank()) return null;
        // Pattern: "Attachment: filename (/path/to/file)" or "Attachments: ...".
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Attachments?:\\s*[^(]*\\(([^)]+)\\)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(body);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1).trim();
        }
        return lastMatch;
    }
}
    

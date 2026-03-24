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

    public List<FAQQuestionDto> getUnansweredNotificationDtos() {
        return getUnansweredNotifications().stream().map(this::toDto).toList();
    }

    private FAQQuestionDto toDto(FAQQuestion entity) {
        FAQQuestionDto dto = new FAQQuestionDto();
        dto.setId(entity.getId());
        dto.setQuestion(extractQuestionTitle(entity.getQuestion()));
        dto.setDetails(extractQuestionDetails(entity.getQuestion()));
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
        String baseUrl = normalizeUrl(supportFrontendBaseUrl, fallbackFrontendBaseUrl);
        return baseUrl + "/faq/" + id;
    }

    private String stripAttachmentLines(String value) {
        if (value == null) return "";
        return value
                .replaceAll("(?im)^\\s*attachments?\\s*:\\s*.*$", "")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
    }

    private String extractQuestionTitle(String rawQuestion) {
        String cleaned = stripAttachmentLines(rawQuestion);
        if (cleaned.isBlank()) return "";
        String[] lines = cleaned.split("\\r?\\n");
        for (String line : lines) {
            if (!line.isBlank()) {
                return line.trim();
            }
        }
        return cleaned.trim();
    }

    private String extractQuestionDetails(String rawQuestion) {
        String cleaned = stripAttachmentLines(rawQuestion);
        if (cleaned.isBlank()) return "";

        String[] lines = cleaned.split("\\r?\\n");
        if (lines.length <= 1) {
            return "";
        }

        StringBuilder details = new StringBuilder();
        boolean started = false;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (!started && line.isBlank()) {
                continue;
            }
            started = true;
            if (details.length() > 0) {
                details.append("\n");
            }
            details.append(line);
        }
        return details.toString().trim();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String toHtmlLines(String value) {
        return escapeHtml(value).replace("\n", "<br>");
    }

    private String faqDetailRow(String label, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "<tr>" +
                "<td style='padding:8px 0; vertical-align:top; color:#6b7280; font-weight:600; width:170px;'>" + escapeHtml(label) + "</td>" +
                "<td style='padding:8px 0; color:#111827; font-weight:500;'>" + toHtmlLines(value) + "</td>" +
                "</tr>";
    }

    private String buildFaqEmailTemplate(String title, String intro, String detailRows, String sectionTitle, String sectionBody, String ctaText, String ctaUrl) {
        String detailsSection = (detailRows != null && !detailRows.isBlank())
                ? "<table style='width:100%; border-collapse:collapse; margin:16px 0;'>" + detailRows + "</table>"
                : "";
        String section = (sectionBody != null && !sectionBody.isBlank())
                ? "<div style='margin-top:14px; background:#f8fafc; border:1px solid #e5e7eb; border-radius:10px; padding:14px 16px;'>" +
                    "<p style='margin:0 0 8px; font-weight:700; color:#1f2937;'>" + escapeHtml(sectionTitle != null ? sectionTitle : "Details") + "</p>" +
                    "<p style='margin:0; color:#374151;'>" + toHtmlLines(sectionBody) + "</p>" +
                  "</div>"
                : "";
        String button = (ctaText != null && !ctaText.isBlank() && ctaUrl != null && !ctaUrl.isBlank())
                ? "<p style='text-align:center; margin:24px 0 0;'><a href='" + ctaUrl + "' style='display:inline-block; padding:12px 22px; background:#0b72f5; color:#fff; border-radius:8px; text-decoration:none; font-weight:700;'>" + escapeHtml(ctaText) + "</a></p>"
                : "";

        return "<div style='font-family: Inter, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, sans-serif; background:#f4f6fb; padding:24px;'>" +
                "<div style='max-width:640px; margin:0 auto; background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 5px 20px rgba(30, 40, 60, .08);'>" +
                "<div style='padding:20px; background:linear-gradient(140deg, #0b72f5 0%, #10b981 100%); color:#fff;'>" +
                "<h2 style='margin:0; font-size:22px; font-weight:800; letter-spacing:.4px;'>" + escapeHtml(title) + "</h2>" +
                "</div>" +
                "<div style='padding:20px; color:#1f2937; line-height:1.6; font-size:15px;'>" +
                "<p style='margin:0 0 12px; color:#374151;'>" + escapeHtml(intro) + "</p>" +
                detailsSection +
                section +
                button +
                "<hr style='margin:24px 0; border:none; border-top:1px solid #e5e7eb;'/>" +
                "<p style='margin:0; font-size:13px; color:#6b7280;'>Need help? Contact <a href='mailto:support@farm-eazy.com'>support@farm-eazy.com</a>.</p>" +
                "</div></div></div>";
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
            String adminBody = buildFaqEmailTemplate(
                "FAQ feedback received",
                "A user submitted feedback on an answered FAQ thread.",
                faqDetailRow("Question ID", String.valueOf(id)) +
                    faqDetailRow("User", senderEmail != null ? senderEmail : entity.getEmail()) +
                    faqDetailRow("Satisfied", String.valueOf(satisfied)),
                "Feedback",
                feedbackText,
                "Open FAQ thread",
                buildFaqUrl(id)
            );
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
        String rawQuestion = dto.getQuestion() != null ? dto.getQuestion().trim() : "";
        String questionTitle = extractQuestionTitle(rawQuestion);
        String questionDetails = (dto.getDetails() != null && !dto.getDetails().isBlank())
            ? stripAttachmentLines(dto.getDetails())
            : extractQuestionDetails(rawQuestion);

        FAQQuestion entity = new FAQQuestion();
        entity.setQuestion(questionTitle.isBlank() ? rawQuestion : questionTitle);
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

        if (questionDetails != null && !questionDetails.isBlank()) {
            FAQCommunication detailsComm = new FAQCommunication();
            detailsComm.setFaqQuestion(entity);
            detailsComm.setRecipientEmail(entity.getEmail());
            detailsComm.setSubject("Additional question details");
            detailsComm.setPurpose("Question Details");
            detailsComm.setBody(questionDetails);
            detailsComm.setSentAt(OffsetDateTime.now());
            faqCommunicationRepository.save(detailsComm);
        }

        notifyFaqUser(
            entity,
            "FAQ question submitted",
            "Your question was received and is pending review.",
            NotificationPriority.NORMAL
        );

        // Send user email notification on submission
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String subject = "Your FarmEazy Question Has Been Received!";
            String body = buildFaqEmailTemplate(
                "We received your FAQ question",
                "Thank you for contacting FarmEazy Support. Our team will review your question and respond soon.",
                faqDetailRow("Question ID", String.valueOf(entity.getId())) +
                        faqDetailRow("Submitted by", entity.getEmail()) +
                        faqDetailRow("Source", entity.getSource()),
                "Your question",
                extractQuestionTitle(entity.getQuestion()),
                "View FAQ section",
                normalizeUrl(supportFrontendBaseUrl, fallbackFrontendBaseUrl) + "/faq"
            );
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
        String supportUrl = normalizeUrl(supportFrontendBaseUrl, fallbackFrontendBaseUrl);
        String faqLink = buildFaqUrl(id);
        if (addToFAQ) {
            subject = "🎉 Your Question Is Now Featured in FarmEazy FAQ!";
            body = buildFaqEmailTemplate(
                "Your question is now published in FAQ",
                "Great news. Your question has been approved and added to the FarmEazy FAQ to help other users.",
                faqDetailRow("Response context", answerContextLabel) +
                        faqDetailRow("Question ID", String.valueOf(id)),
                "Question and answer",
                "Question: " + extractQuestionTitle(entity.getQuestion()) + "\n\nAnswer: " + htmlToPlainText(answer),
                "View published FAQ",
                faqLink
            );
            storeCommunication(entity, subject, body, "Admin Reply - " + answerContextLabel + " (Published)");
            try {
                httpEmailService.sendEmail(recipientEmail, subject, body);
            } catch (Exception e) {
                System.err.println("Failed to send FAQ addition email to user: " + e.getMessage());
            }
        } else {
            subject = "✅ Response to Your FarmEazy Question";
            body = buildFaqEmailTemplate(
                "Response to your FarmEazy question",
                "Our support team has reviewed your question and shared an answer below.",
                faqDetailRow("Response context", answerContextLabel) +
                        faqDetailRow("Question ID", String.valueOf(id)),
                "Question and answer",
                "Question: " + extractQuestionTitle(entity.getQuestion()) + "\n\nAnswer: " + htmlToPlainText(answer),
                "Raise a support ticket",
                supportUrl + "/support"
            );
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
                String adminBody = buildFaqEmailTemplate(
                    "New FAQ follow-up received",
                    "A user reopened a FAQ thread with a follow-up question.",
                    faqDetailRow("Question ID", String.valueOf(id)) +
                        faqDetailRow("User", requester != null && !requester.isBlank() ? requester : "Guest"),
                    "Follow-up question",
                        "Original question: " + extractQuestionTitle(entity.getQuestion()) + "\n\nFollow-up: " + userSubQuestion,
                    "Open FAQ thread",
                    buildFaqUrl(id)
                );
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
        String body = "Question: " + extractQuestionTitle(entity.getQuestion()) + "\nCancelled by: " + adminEmail + " on " + OffsetDateTime.now();
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
    

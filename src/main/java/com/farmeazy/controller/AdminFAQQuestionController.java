package com.farmeazy.controller;

import com.farmeazy.dto.FAQQuestionDto;
import com.farmeazy.entity.FAQQuestion;
import com.farmeazy.service.FAQQuestionService;
import com.farmeazy.service.FileStorageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/admin/faq-questions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminFAQQuestionController {

    @Autowired
    private FAQQuestionService faqQuestionService;

    @Autowired
    private FileStorageService fileStorageService;

    private String appendAttachments(String answer, MultipartFile[] files, MultipartFile file) {
        String base = answer != null ? answer : "";
        List<String> lines = new ArrayList<>();
        if (files != null) {
            for (MultipartFile item : files) {
                if (item == null || item.isEmpty()) continue;
                String storedName = fileStorageService.store(item);
                String originalName = (item.getOriginalFilename() != null && !item.getOriginalFilename().isBlank())
                        ? item.getOriginalFilename()
                        : storedName;
                lines.add("Attachment: " + originalName + " (/uploads/" + storedName + ")");
            }
        }
        if (file != null && !file.isEmpty()) {
            String storedName = fileStorageService.store(file);
            String originalName = (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                    ? file.getOriginalFilename()
                    : storedName;
            lines.add("Attachment: " + originalName + " (/uploads/" + storedName + ")");
        }
        if (lines.isEmpty()) {
            return base;
        }
        String attachmentText = String.join("\n", lines);
        return base.isBlank() ? attachmentText : (base + "\n\n" + attachmentText);
    }

    @GetMapping
    public ResponseEntity<List<FAQQuestionDto>> getAllQuestions() {
        // support query param ?unanswered=true to fetch only new notifications
        // Note: Spring will map query params automatically via @RequestParam if provided; here we read from request
        return ResponseEntity.ok(faqQuestionService.getAllQuestionsForAdmin());
    }

    @GetMapping(path = "", params = "unanswered=true")
    public ResponseEntity<List<FAQQuestion>> getUnansweredNotifications() {
        return ResponseEntity.ok(faqQuestionService.getUnansweredNotifications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FAQQuestionDto> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(faqQuestionService.getPublicFaqById(id));
    }

    @PostMapping(value = "/{id}/answer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> answerQuestion(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String answer = (String) payload.get("answer");
        boolean addToFAQ = Boolean.TRUE.equals(payload.get("addToFAQ"));
        faqQuestionService.answerQuestion(id, answer, addToFAQ);
        return ResponseEntity.ok(Map.of("message", addToFAQ ? "Question added to FAQ and user notified." : "Answer sent to user."));
    }

    @PostMapping(value = "/{id}/answer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> answerQuestionMultipart(
            @PathVariable Long id,
            @RequestParam String answer,
            @RequestParam(required = false) Boolean addToFAQ,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        boolean publish = Boolean.TRUE.equals(addToFAQ);
        String composedAnswer = appendAttachments(answer, files, file);
        faqQuestionService.answerQuestion(id, composedAnswer, publish);
        return ResponseEntity.ok(Map.of("message", publish ? "Question added to FAQ and user notified." : "Answer sent to user."));
    }

    @PostMapping(value = "/{id}/answer-only", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> answerOnly(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String answer = (String) payload.get("answer");
        faqQuestionService.answerQuestion(id, answer, false);
        return ResponseEntity.ok(Map.of("message", "Answer added to history and user notified (no FAQ publish)."));
    }

    @PostMapping(value = "/{id}/answer-only", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> answerOnlyMultipart(
            @PathVariable Long id,
            @RequestParam String answer,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        String composedAnswer = appendAttachments(answer, files, file);
        faqQuestionService.answerQuestion(id, composedAnswer, false);
        return ResponseEntity.ok(Map.of("message", "Answer added to history and user notified (no FAQ publish)."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteQuestion(@PathVariable Long id) {
        faqQuestionService.deleteQuestion(id);
        return ResponseEntity.ok(Map.of("message", "Question deleted."));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelReview(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        // payload may contain adminEmail if available
        String adminEmail = payload != null && payload.get("adminEmail") != null ? (String) payload.get("adminEmail") : "admin@farm-eazy.com";
        faqQuestionService.recordAdminCancel(id, adminEmail);
        return ResponseEntity.ok(Map.of("message", "Review cancelled and recorded in history."));
    }

    @PostMapping("/{id}/mark-read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable Long id) {
        faqQuestionService.markNotificationRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked read."));
    }

    @PostMapping("/{id}/internal-note")
    public ResponseEntity<FAQQuestionDto> addInternalNote(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String note = body.getOrDefault("note", "");
        String author = body.getOrDefault("author", "admin");
        return ResponseEntity.ok(faqQuestionService.addInternalNote(id, note, author));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<FAQQuestionDto> reopenQuestion(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        String requester = body != null ? body.getOrDefault("requester", "admin") : "admin";
        String subQuestion = body != null ? body.getOrDefault("subQuestion", "") : "";
        return ResponseEntity.ok(faqQuestionService.reopenQuestion(id, requester, subQuestion));
    }

    @PostMapping("/{id}/solved")
    public ResponseEntity<FAQQuestionDto> markSolved(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        String resolver = body != null ? body.getOrDefault("resolver", "admin") : "admin";
        return ResponseEntity.ok(faqQuestionService.markQuestionSolved(id, resolver));
    }
}

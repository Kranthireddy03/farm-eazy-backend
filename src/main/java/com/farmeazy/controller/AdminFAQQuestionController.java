package com.farmeazy.controller;

import com.farmeazy.entity.FAQQuestion;
import com.farmeazy.service.FAQQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/faq-questions")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
public class AdminFAQQuestionController {

    @Autowired
    private FAQQuestionService faqQuestionService;

    @GetMapping
    public ResponseEntity<List<FAQQuestion>> getAllQuestions() {
        // support query param ?unanswered=true to fetch only new notifications
        // Note: Spring will map query params automatically via @RequestParam if provided; here we read from request
        return ResponseEntity.ok(faqQuestionService.getAllQuestions());
    }

    @GetMapping(path = "", params = "unanswered=true")
    public ResponseEntity<List<FAQQuestion>> getUnansweredNotifications() {
        return ResponseEntity.ok(faqQuestionService.getUnansweredNotifications());
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<Map<String, String>> answerQuestion(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String answer = (String) payload.get("answer");
        boolean addToFAQ = Boolean.TRUE.equals(payload.get("addToFAQ"));
        faqQuestionService.answerQuestion(id, answer, addToFAQ);
        return ResponseEntity.ok(Map.of("message", addToFAQ ? "Question added to FAQ and user notified." : "Answer sent to user."));
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
}

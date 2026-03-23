package com.farmeazy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.farmeazy.dto.FAQQuestionDto;
import com.farmeazy.service.FAQQuestionService;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "http://localhost:4200",
    "http://localhost:3000",
    "http://localhost:3001",
    "http://localhost:5173"
})
public class PublicFAQController {
    private boolean hasAnyAttachment(MultipartFile[] files, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            return true;
        }
        if (files != null) {
            for (MultipartFile item : files) {
                if (item != null && !item.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @PostMapping({"/public/faq-question", "/faq-question"})
    public ResponseEntity<String> submitPublicFaqQuestion(@RequestBody FAQQuestionDto dto) {
        if (dto.getSource() == null || dto.getSource().isBlank()) {
            if (dto.getUserId() != null && !dto.getUserId().isBlank()) {
                dto.setSource("FAQ_USER_APP");
            } else {
                dto.setSource("FAQ_PUBLIC_PAGE");
            }
        }
        faqQuestionService.processQuestion(dto);
        return ResponseEntity.ok("Your question has been submitted successfully.");
    }

    @PostMapping(value = {"/public/faq-question", "/faq-question"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> submitPublicFaqQuestionMultipart(
            @RequestParam String question,
            @RequestParam String email,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String userId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        if (hasAnyAttachment(files, file)) {
            return ResponseEntity.badRequest().body("Attachments are disabled for public FAQ submissions. Please sign in to attach files.");
        }

        FAQQuestionDto dto = new FAQQuestionDto();
        dto.setEmail(email);
        dto.setUserId(userId);
        dto.setSource((source == null || source.isBlank()) ? "FAQ_PUBLIC_PAGE" : source);
        dto.setQuestion(question);

        faqQuestionService.processQuestion(dto);
        return ResponseEntity.ok("Your question has been submitted successfully.");
    }

    @Autowired
    private FAQQuestionService faqQuestionService;

    @GetMapping({"/public/faq-questions", "/faq-questions"})
    public ResponseEntity<List<FAQQuestionDto>> getPublicFaqs() {
        List<FAQQuestionDto> faqs = faqQuestionService.getAllApprovedFaqs();
        return ResponseEntity.ok(faqs);
    }

    @GetMapping({"/public/faq-question/{id}", "/faq-question/{id}"})
    public ResponseEntity<FAQQuestionDto> getPublicFaq(@PathVariable Long id) {
        return ResponseEntity.ok(faqQuestionService.getPublicFaqById(id));
    }

    @PostMapping({"/public/faq-question/{id}/feedback", "/faq-question/{id}/feedback"})
    public ResponseEntity<FAQQuestionDto> submitFaqFeedback(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body) {
        Boolean satisfied = Boolean.valueOf(String.valueOf(body.getOrDefault("satisfied", "false")));
        String feedback = body.getOrDefault("feedback", "").toString();
        String email = body.getOrDefault("email", "").toString();
        return ResponseEntity.ok(faqQuestionService.submitFaqFeedback(id, satisfied, feedback, email));
    }

    @PostMapping(value = {"/public/faq-question/{id}/feedback", "/faq-question/{id}/feedback"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FAQQuestionDto> submitFaqFeedbackMultipart(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean satisfied,
            @RequestParam(required = false) String feedback,
            @RequestParam(required = false) String email,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        if (hasAnyAttachment(files, file)) {
            throw new IllegalArgumentException("Attachments are disabled for public FAQ feedback. Please sign in to attach files.");
        }

        return ResponseEntity.ok(faqQuestionService.submitFaqFeedback(id, Boolean.TRUE.equals(satisfied), feedback != null ? feedback : "", email != null ? email : ""));
    }

    @PostMapping({"/public/faq-question/{id}/reopen", "/faq-question/{id}/reopen"})
    public ResponseEntity<FAQQuestionDto> reopenFaq(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        String requester = body != null ? body.getOrDefault("requester", "public") : "public";
        String userSubQuestion = body != null ? body.getOrDefault("subQuestion", "") : "";
        return ResponseEntity.ok(faqQuestionService.reopenQuestion(id, requester, userSubQuestion));
    }

    @PostMapping(value = {"/public/faq-question/{id}/reopen", "/faq-question/{id}/reopen"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FAQQuestionDto> reopenFaqWithAttachments(
            @PathVariable Long id,
            @RequestParam(required = false) String requester,
            @RequestParam(required = false) String subQuestion,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        if (hasAnyAttachment(files, null)) {
            throw new IllegalArgumentException("Attachments are disabled for public FAQ follow-up. Please sign in to attach files.");
        }

        String resolvedRequester = (requester != null && !requester.isBlank()) ? requester : "public";
        String baseText = (subQuestion != null) ? subQuestion.trim() : "";

        return ResponseEntity.ok(faqQuestionService.reopenQuestion(id, resolvedRequester, baseText));
    }

    @PostMapping({"/public/faq-question/{id}/solved", "/faq-question/{id}/solved"})
    public ResponseEntity<FAQQuestionDto> markFaqSolved(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        String resolver = body != null ? body.getOrDefault("resolver", "public") : "public";
        return ResponseEntity.ok(faqQuestionService.markQuestionSolved(id, resolver));
    }
}


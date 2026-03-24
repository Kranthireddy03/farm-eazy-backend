package com.farmeazy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import com.farmeazy.dto.FAQQuestionDto;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.service.FAQQuestionService;
import com.farmeazy.service.FileStorageService;
import com.farmeazy.repository.UserRepository;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/faq/question")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
public class FAQQuestionController {

    private String requireAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Authentication is required");
        }
        return authentication.getName();
    }

    private String appendAttachments(String question, MultipartFile[] files, MultipartFile file) {
        String base = question != null ? question : "";
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

    @Autowired
    private FAQQuestionService faqQuestionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<String> submitQuestion(@Valid @RequestBody FAQQuestionDto dto, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String authEmail = authentication.getName();
            dto.setEmail(authEmail);
            userRepository.findByEmail(authEmail).ifPresent(user -> dto.setUserId(String.valueOf(user.getId())));
        }
        faqQuestionService.processQuestion(dto);
        return ResponseEntity.ok("Thank you for your question. Our admin team will reply via email.");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> submitQuestionMultipart(
            @RequestParam String question,
            @RequestParam(required = false) String details,
            @RequestParam(required = false) String source,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {

        FAQQuestionDto dto = new FAQQuestionDto();
        dto.setQuestion(question);
        dto.setDetails(details);
        dto.setSource(source);

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String authEmail = authentication.getName();
            dto.setEmail(authEmail);
            userRepository.findByEmail(authEmail).ifPresent(user -> dto.setUserId(String.valueOf(user.getId())));
        }

        dto.setDetails(appendAttachments(details != null ? details : "", files, file));

        faqQuestionService.processQuestion(dto);
        return ResponseEntity.ok("Thank you for your question. Our admin team will reply via email.");
    }

    @GetMapping("/my")
    public ResponseEntity<List<FAQQuestionDto>> getMyQuestions(Authentication authentication) {
        String email = null;
        String userId = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            email = authentication.getName();
            userId = userRepository.findByEmail(email).map(u -> String.valueOf(u.getId())).orElse(null);
        }
        return ResponseEntity.ok(faqQuestionService.getQuestionsForUser(email, userId));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<FAQQuestionDto> submitAuthenticatedFeedback(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            Authentication authentication) {
        String email = requireAuthenticatedEmail(authentication);
        boolean satisfied = Boolean.parseBoolean(String.valueOf(body != null ? body.getOrDefault("satisfied", false) : false));
        String feedback = String.valueOf(body != null ? body.getOrDefault("feedback", "") : "");
        return ResponseEntity.ok(faqQuestionService.submitFaqFeedback(id, satisfied, feedback, email));
    }

    @PostMapping(value = "/{id}/feedback", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FAQQuestionDto> submitAuthenticatedFeedbackMultipart(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean satisfied,
            @RequestParam(required = false) String feedback,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        String email = requireAuthenticatedEmail(authentication);
        String feedbackText = appendAttachments(feedback != null ? feedback : "", files, file);
        return ResponseEntity.ok(faqQuestionService.submitFaqFeedback(id, Boolean.TRUE.equals(satisfied), feedbackText, email));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<FAQQuestionDto> reopenAuthenticatedFaq(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            Authentication authentication) {
        String email = requireAuthenticatedEmail(authentication);
        String subQuestion = body != null ? body.getOrDefault("subQuestion", "") : "";
        return ResponseEntity.ok(faqQuestionService.reopenQuestion(id, email, subQuestion));
    }

    @PostMapping(value = "/{id}/reopen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FAQQuestionDto> reopenAuthenticatedFaqMultipart(
            @PathVariable Long id,
            @RequestParam(required = false) String subQuestion,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {
        String email = requireAuthenticatedEmail(authentication);
        String composed = appendAttachments(subQuestion != null ? subQuestion : "", files, file);
        return ResponseEntity.ok(faqQuestionService.reopenQuestion(id, email, composed));
    }

    @PostMapping("/{id}/solved")
    public ResponseEntity<FAQQuestionDto> markAuthenticatedFaqSolved(
            @PathVariable Long id,
            Authentication authentication) {
        String email = requireAuthenticatedEmail(authentication);
        return ResponseEntity.ok(faqQuestionService.markQuestionSolved(id, email));
    }

}

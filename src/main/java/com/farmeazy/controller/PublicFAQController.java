package com.farmeazy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @Autowired
    private FAQQuestionService faqQuestionService;

    @GetMapping("/faq-questions")
    public ResponseEntity<List<FAQQuestionDto>> getPublicFaqs() {
        List<FAQQuestionDto> faqs = faqQuestionService.getAllApprovedFaqs();
        return ResponseEntity.ok(faqs);
    }
}

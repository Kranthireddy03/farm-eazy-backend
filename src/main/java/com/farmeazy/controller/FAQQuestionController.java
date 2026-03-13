package com.farmeazy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.farmeazy.dto.FAQQuestionDto;
import com.farmeazy.service.FAQQuestionService;

@RestController
@RequestMapping("/api/faq/question")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
public class FAQQuestionController {

    @Autowired
    private FAQQuestionService faqQuestionService;

    @PostMapping
    public ResponseEntity<String> submitQuestion(@Valid @RequestBody FAQQuestionDto dto) {
        faqQuestionService.processQuestion(dto);
        return ResponseEntity.ok("Thank you for your question. Our admin team will reply via email.");
    }
}

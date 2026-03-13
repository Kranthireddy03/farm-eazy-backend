package com.farmeazy.controller;

import com.farmeazy.entity.FAQCommunication;
import com.farmeazy.repository.FAQCommunicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/faq-history")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
public class AdminFAQHistoryController {

    @Autowired
    private FAQCommunicationRepository faqCommunicationRepository;

    @GetMapping
    public ResponseEntity<List<FAQCommunication>> getHistory() {
        List<FAQCommunication> list = faqCommunicationRepository.findAll();
        return ResponseEntity.ok(list);
    }
}

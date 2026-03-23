package com.farmeazy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.farmeazy.dto.PublicSupportMessageDto;
import com.farmeazy.service.PublicSupportMessageService;

@RestController
@RequestMapping("/api/public/support-message")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
public class PublicSupportMessageController {

    @Autowired
    private PublicSupportMessageService supportMessageService;

    @PostMapping
    public ResponseEntity<String> sendSupportMessage(@Valid @RequestBody PublicSupportMessageDto dto) {
        supportMessageService.processPublicMessage(dto);
        return ResponseEntity.ok("Message received. Our team will contact you soon.");
    }
}

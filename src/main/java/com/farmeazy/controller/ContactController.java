package com.farmeazy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.farmeazy.dto.ContactMessageDto;
import com.farmeazy.service.ContactService;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    public ResponseEntity<String> sendMessage(@Valid @RequestBody ContactMessageDto dto) {
        contactService.processMessage(dto);
        return ResponseEntity.ok("Message received. Our team will contact you soon.");
    }
}

package com.farmeazy.controller;

import com.farmeazy.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
public class EmailAlertController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/email")
    public String sendEmailAlert(@RequestParam String to, @RequestParam String subject, @RequestParam String message) {
        emailService.sendEmail(to, subject, message);
        return "Email alert sent";
    }
}

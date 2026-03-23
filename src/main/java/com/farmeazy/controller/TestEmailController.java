package com.farmeazy.controller;

import com.farmeazy.service.UnifiedEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-email")
public class TestEmailController {
    @GetMapping("/zoho")
    public String sendZohoTestEmailGet(@RequestParam String to, @RequestParam String name) {
        boolean result = unifiedEmailService.sendWelcomeEmail(to, name);
        return result ? "Zoho email sent successfully to " + to : "Zoho email failed for " + to;
    }

    @Autowired
    private UnifiedEmailService unifiedEmailService;

    @PostMapping("/zoho")
    public String sendZohoTestEmail(@RequestParam String to, @RequestParam String name) {
        boolean result = unifiedEmailService.sendWelcomeEmail(to, name);
        return result ? "Zoho email sent successfully to " + to : "Zoho email failed for " + to;
    }
}

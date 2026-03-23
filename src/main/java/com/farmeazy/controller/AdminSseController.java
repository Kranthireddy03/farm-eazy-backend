package com.farmeazy.controller;

import com.farmeazy.service.NotificationSseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/faq-questions")
public class AdminSseController {

    private final NotificationSseService notificationSseService;

    public AdminSseController(NotificationSseService notificationSseService) {
        this.notificationSseService = notificationSseService;
    }

    @PostMapping("/stream-token")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public Map<String, Object> createStreamToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        // create token valid for 2 minutes
        String token = notificationSseService.createTokenForUser(auth.getName(), 2 * 60 * 1000);
        long expiresAt = System.currentTimeMillis() + (2 * 60 * 1000);
        return Map.of("token", token, "expiresAt", expiresAt);
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam(name = "token", required = true) String token) {
        java.util.Optional<String> owner = notificationSseService.validateAndConsumeToken(token);
        if (owner.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired SSE token");
        }
        return notificationSseService.createEmitter(owner.get());
    }
}

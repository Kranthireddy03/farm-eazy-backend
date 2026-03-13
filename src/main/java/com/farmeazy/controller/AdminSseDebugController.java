package com.farmeazy.controller;

import com.farmeazy.service.NotificationSseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/faq-questions")
public class AdminSseDebugController {

    private final NotificationSseService notificationSseService;

    public AdminSseDebugController(NotificationSseService notificationSseService) {
        this.notificationSseService = notificationSseService;
    }

    @GetMapping("/stream-status")
    public Map<String, Object> status() {
        return Map.of(
            "emitters", notificationSseService.getEmitterCount(),
            "lastPayload", notificationSseService.getLastPayloadJson(),
            "emitterDetails", notificationSseService.getEmitterDetails()
        );
    }

    // Admin-only test endpoint to emit a synthetic notification to all connected SSE emitters.
    @PostMapping("/stream-test")
    public Map<String, Object> triggerTestNotification() {
        // Use a mutable map (allows nulls) or provide empty string for 'answer' to avoid Map.of(null) NPE
        java.util.Map<String, Object> item = new java.util.HashMap<>();
        item.put("id", -1);
        item.put("question", "Synthetic test notification");
        item.put("answer", "");
        item.put("submittedAt", Instant.now().toString());

        List<java.util.Map<String, Object>> payload = List.of(item);

        notificationSseService.sendNotifications(payload);

        return Map.of(
            "status", "sent",
            "emitters", notificationSseService.getEmitterCount(),
            "lastPayload", notificationSseService.getLastPayloadJson()
        );
    }
}

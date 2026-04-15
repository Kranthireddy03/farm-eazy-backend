package com.farmeazy.controller;

import com.farmeazy.service.SystemStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/full-status")
    public ResponseEntity<Map<String, Object>> fullStatus() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("generatedAt", Instant.now().toString());
        body.put("services", systemStatusService.getFullStatus());
        return ResponseEntity.ok(body);
    }
}

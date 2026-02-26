package com.farmeazy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Home Controller - Welcome page for the API
 * 
 * Provides a simple welcome response when accessing the base URL.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "FarmEazy Backend API");
        response.put("version", "1.0.0");
        response.put("status", "Running");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "Welcome to FarmEazy - Smart Farm Management API");
        response.put("documentation", "/swagger-ui.html");
        response.put("endpoints", Map.of(
            "register", "/api/auth/register",
            "login", "/api/auth/login",
            "farms", "/api/farms",
            "crops", "/api/crops"
        ));
        return ResponseEntity.ok(response);
    }

}

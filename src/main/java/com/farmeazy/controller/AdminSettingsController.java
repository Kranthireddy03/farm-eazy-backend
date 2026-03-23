package com.farmeazy.controller;

import com.farmeazy.entity.AppSettings;
import com.farmeazy.service.AppSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "Admin Settings", description = "Global system settings for FarmEazy admin")
public class AdminSettingsController {

    @Autowired
    private AppSettingsService appSettingsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    @Operation(summary = "Get current settings")
    public ResponseEntity<AppSettings> getSettings() {
        return ResponseEntity.ok(appSettingsService.getSettings());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
    @Operation(summary = "Update settings")
    public ResponseEntity<AppSettings> updateSettings(@RequestBody AppSettings settings) {
        return ResponseEntity.ok(appSettingsService.updateSettings(settings));
    }
}

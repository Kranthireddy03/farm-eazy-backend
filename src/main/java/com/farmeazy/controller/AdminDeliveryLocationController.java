package com.farmeazy.controller;

import com.farmeazy.dto.DeliveryLocationCreateDto;
import com.farmeazy.dto.DeliveryLocationDto;
import com.farmeazy.service.DeliveryLocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/delivery-locations")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:3000",
    "http://localhost:4200",
    "http://localhost:5173"
})
public class AdminDeliveryLocationController {

    private final DeliveryLocationService deliveryLocationService;

    public AdminDeliveryLocationController(DeliveryLocationService deliveryLocationService) {
        this.deliveryLocationService = deliveryLocationService;
    }

    @GetMapping
    public ResponseEntity<List<DeliveryLocationDto>> getAllLocations() {
        return ResponseEntity.ok(deliveryLocationService.getAllLocations());
    }

    @GetMapping("/active")
    public ResponseEntity<List<DeliveryLocationDto>> getActiveLocations() {
        return ResponseEntity.ok(deliveryLocationService.getActiveLocations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryLocationDto> getLocation(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryLocationService.getLocation(id));
    }

    @PostMapping
    public ResponseEntity<DeliveryLocationDto> createLocation(@Valid @RequestBody DeliveryLocationCreateDto request) {
        return ResponseEntity.ok(deliveryLocationService.createLocation(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryLocationDto> updateLocation(@PathVariable Long id, @Valid @RequestBody DeliveryLocationCreateDto request) {
        return ResponseEntity.ok(deliveryLocationService.updateLocation(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeliveryLocationDto> updateStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(deliveryLocationService.setActive(id, active));
    }
}
package com.farmeazy.controller;

import com.farmeazy.dto.LocationAccessStatusDto;
import com.farmeazy.service.DeliveryLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/location-access")
public class LocationAccessController {

    private final DeliveryLocationService deliveryLocationService;

    public LocationAccessController(DeliveryLocationService deliveryLocationService) {
        this.deliveryLocationService = deliveryLocationService;
    }

    @GetMapping("/status")
    public ResponseEntity<LocationAccessStatusDto> getLocationAccessStatus(
        @RequestHeader(value = "X-User-Location", required = false) String userLocationHeader
    ) {
        return ResponseEntity.ok(deliveryLocationService.getLocationAccessStatus(userLocationHeader));
    }
}

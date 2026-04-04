package com.farmeazy.controller;

import com.farmeazy.entity.User;
import com.farmeazy.service.ListingEligibilityService;
import com.farmeazy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/vendors")
@CrossOrigin(origins = {
        "https://farm-eazy.com",
        "https://www.farm-eazy.com",
        "https://farm-eazy.vercel.app",
        "http://localhost:3000",
        "http://localhost:4200",
        "http://localhost:5173"
})
public class VendorEligibilityController {

    @Autowired
    private ListingEligibilityService listingEligibilityService;

    @Autowired
    private UserService userService;

    @GetMapping("/listing-eligibility")
    public ResponseEntity<Map<String, Object>> getListingEligibility(
            Principal principal,
            @RequestParam(defaultValue = "PRODUCT") String listingType) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        Map<String, Object> response = listingEligibilityService.getEligibility(user, listingType);
        return ResponseEntity.ok(response);
    }
}

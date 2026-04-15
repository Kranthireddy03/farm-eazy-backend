package com.farmeazy.controller;

import com.farmeazy.dto.VendorOnboardingProfileDto;
import com.farmeazy.dto.VendorOnboardingProfileUpdateDto;
import com.farmeazy.entity.User;
import com.farmeazy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

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
public class VendorOnboardingController {

    @Autowired
    private UserService userService;

    @GetMapping("/onboarding-profile")
    public ResponseEntity<VendorOnboardingProfileDto> getOnboardingProfile(Principal principal) {
        User user = resolveUser(principal);

        VendorOnboardingProfileDto dto = new VendorOnboardingProfileDto();
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setCity(user.getCity());
        dto.setState(user.getState());
        dto.setPinCode(user.getPinCode());

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/onboarding-profile")
    public ResponseEntity<VendorOnboardingProfileDto> updateOnboardingProfile(
            Principal principal,
            @Valid @RequestBody VendorOnboardingProfileUpdateDto request) {
        User user = resolveUser(principal);
        User updated = userService.updateVendorOnboardingProfile(
                user,
                request.getPhone(),
                request.getAddress(),
                request.getCity(),
                request.getState(),
                request.getPinCode());

        VendorOnboardingProfileDto dto = new VendorOnboardingProfileDto();
        dto.setEmail(updated.getEmail());
        dto.setPhone(updated.getPhone());
        dto.setAddress(updated.getAddress());
        dto.setCity(updated.getCity());
        dto.setState(updated.getState());
        dto.setPinCode(updated.getPinCode());

        return ResponseEntity.ok(dto);
    }

    private User resolveUser(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }
}

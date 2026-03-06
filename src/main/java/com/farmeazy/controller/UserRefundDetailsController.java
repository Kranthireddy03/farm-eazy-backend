package com.farmeazy.controller;

import com.farmeazy.dto.UserRefundDetailsDto;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.UserRefundDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Controller for managing user refund details (bank/UPI info for receiving refunds).
 */
@RestController
@RequestMapping("/api/refund-details")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
@Tag(name = "Refund Details", description = "Manage buyer's bank/UPI details for refunds")
public class UserRefundDetailsController {

    @Autowired
    private UserRefundDetailsService refundDetailsService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Save or update refund details for the logged-in user.
     */
    @PostMapping("/save")
    @Operation(summary = "Save or update refund details", description = "Save or update bank/UPI details for receiving refunds")
    public ResponseEntity<?> saveRefundDetails(
            Authentication authentication,
            @Valid @RequestBody UserRefundDetailsDto dto) {
        
        User user = getCurrentUser(authentication);
        UserRefundDetailsDto saved = refundDetailsService.saveOrUpdate(user, dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get refund details for the logged-in user.
     */
    @GetMapping("/my-details")
    @Operation(summary = "Get my refund details", description = "Get saved bank/UPI details for the logged-in user")
    public ResponseEntity<?> getMyRefundDetails(Authentication authentication) {
        User user = getCurrentUser(authentication);
        Optional<UserRefundDetailsDto> details = refundDetailsService.getByUser(user);
        
        if (details.isPresent()) {
            return ResponseEntity.ok(details.get());
        } else {
            return ResponseEntity.ok(Map.of(
                "hasDetails", false,
                "message", "No refund details found. Please add your bank/UPI details."
            ));
        }
    }

    /**
     * Check if user has valid refund details.
     */
    @GetMapping("/check")
    @Operation(summary = "Check refund details", description = "Check if user has valid refund details")
    public ResponseEntity<?> checkRefundDetails(Authentication authentication) {
        User user = getCurrentUser(authentication);
        boolean hasDetails = refundDetailsService.hasValidRefundDetails(user);
        
        return ResponseEntity.ok(Map.of(
            "hasDetails", hasDetails,
            "message", hasDetails ? "Refund details are valid" : "Please add your bank/UPI details"
        ));
    }

    /**
     * Delete refund details for the logged-in user.
     */
    @DeleteMapping("/delete")
    @Operation(summary = "Delete refund details", description = "Delete saved bank/UPI details")
    public ResponseEntity<?> deleteRefundDetails(Authentication authentication) {
        User user = getCurrentUser(authentication);
        refundDetailsService.deleteByUser(user);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Refund details deleted successfully"
        ));
    }

    /**
     * Verify refund details (admin only).
     */
    @PutMapping("/verify/{id}")
    @Operation(summary = "Verify refund details", description = "Admin action to verify user's refund details")
    public ResponseEntity<?> verifyRefundDetails(@PathVariable Long id) {
        // TODO: Add admin role check
        UserRefundDetailsDto verified = refundDetailsService.verifyRefundDetails(id);
        return ResponseEntity.ok(verified);
    }

    /**
     * Get current authenticated user.
     */
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

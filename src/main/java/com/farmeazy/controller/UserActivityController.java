package com.farmeazy.controller;

import com.farmeazy.dto.UserActivityDto;
import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.service.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:3000",
    "http://localhost:4200"
})
public class UserActivityController {

    private final UserActivityService userActivityService;
    private final UserRepository userRepository;

    @Autowired
    public UserActivityController(UserActivityService userActivityService, UserRepository userRepository) {
        this.userActivityService = userActivityService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserActivityDto>> getUserActivities(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<UserActivityDto> activities = userActivityService.getUserActivities(user, pageable).getContent();
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<UserActivityDto>> getRecentActivities(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<UserActivityDto> activities = userActivityService.getRecentActivities(user);
        return ResponseEntity.ok(activities);
    }
}

package com.farmeazy.controller;

import com.farmeazy.dto.DashboardStatsDto;
import com.farmeazy.dto.IrrigationScheduleDto;
import com.farmeazy.service.IrrigationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.farmeazy.repository.UserRepository;
import com.farmeazy.entity.User;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/irrigation")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:4200",
    "http://localhost:3000",
    "http://localhost:3001"
})
@Tag(name = "Irrigation Management", description = "Irrigation schedule operations")
public class IrrigationController {

    private static final Logger logger = LoggerFactory.getLogger(IrrigationController.class);

    @Autowired
    private IrrigationService irrigationService;

    @Autowired
    private UserRepository userRepository;

    private Long getUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return user.getId();
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get all irrigation schedules for user")
    public ResponseEntity<List<IrrigationScheduleDto>> getAllSchedules(
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("IRRIGATION_CONTROLLER_GET_ALL userId={}", userId);
        List<IrrigationScheduleDto> schedules = irrigationService.getAllSchedulesByUser(userId);
        return ResponseEntity.ok(schedules);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a new irrigation schedule")
    public ResponseEntity<IrrigationScheduleDto> createSchedule(
            @Valid @RequestBody IrrigationScheduleDto scheduleDto,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("IRRIGATION_CONTROLLER_CREATE userId={} farmId={} cropId={}", userId, scheduleDto != null ? scheduleDto.getFarmId() : null, scheduleDto != null ? scheduleDto.getCropId() : null);
        IrrigationScheduleDto response = irrigationService.createSchedule(scheduleDto, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{scheduleId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get irrigation schedule by ID")
    public ResponseEntity<IrrigationScheduleDto> getScheduleById(
            @PathVariable Long scheduleId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("IRRIGATION_CONTROLLER_GET_BY_ID userId={} scheduleId={}", userId, scheduleId);
        IrrigationScheduleDto schedule = irrigationService.getScheduleById(scheduleId, userId);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/farm/{farmId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get all irrigation schedules for a farm with pagination")
    public ResponseEntity<Page<IrrigationScheduleDto>> getSchedulesByFarm(
            @PathVariable Long farmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("IRRIGATION_CONTROLLER_GET_BY_FARM userId={} farmId={} page={} size={}", userId, farmId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<IrrigationScheduleDto> schedules = irrigationService.getSchedulesByFarm(farmId, userId, pageable);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get upcoming irrigation schedules")
    public ResponseEntity<List<IrrigationScheduleDto>> getUpcomingSchedules(
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("IRRIGATION_CONTROLLER_GET_UPCOMING userId={}", userId);
        List<IrrigationScheduleDto> schedules = irrigationService.getUpcomingSchedules(userId);
        return ResponseEntity.ok(schedules);
    }

    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update an irrigation schedule")
    public ResponseEntity<IrrigationScheduleDto> updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody IrrigationScheduleDto scheduleDto,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("IRRIGATION_CONTROLLER_UPDATE userId={} scheduleId={}", userId, scheduleId);
        IrrigationScheduleDto response = irrigationService.updateSchedule(scheduleId, scheduleDto, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{scheduleId}/complete")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mark irrigation schedule as completed")
    public ResponseEntity<IrrigationScheduleDto> markAsCompleted(
            @PathVariable Long scheduleId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("IRRIGATION_CONTROLLER_COMPLETE userId={} scheduleId={}", userId, scheduleId);
        IrrigationScheduleDto response = irrigationService.markAsCompleted(scheduleId, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Delete an irrigation schedule")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long scheduleId,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("IRRIGATION_CONTROLLER_DELETE userId={} scheduleId={}", userId, scheduleId);
        irrigationService.deleteSchedule(scheduleId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("IRRIGATION_CONTROLLER_DASHBOARD_STATS userId={}", userId);
        DashboardStatsDto stats = irrigationService.getDashboardStats(userId);
        return ResponseEntity.ok(stats);
    }

}

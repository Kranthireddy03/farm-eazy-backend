package com.farmeazy.controller;

import com.farmeazy.dto.IrrigationRecommendationDto;
import com.farmeazy.dto.IrrigationScheduleDto;
import com.farmeazy.dto.IrrigationHistoryDto;
import com.farmeazy.dto.ApiResponse;
import com.farmeazy.entity.IrrigationSchedule;
import com.farmeazy.entity.IrrigationHistory;
import com.farmeazy.service.SmartIrrigationService;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Smart Irrigation Management System
 * 
 * Endpoints for:
 * - Getting irrigation recommendations
 * - Confirming/skipping irrigation
 * - Viewing irrigation history
 * - Checking system status
 * 
 * All endpoints require authentication (JWT)
 */
@RestController
@RequestMapping("/api/irrigation")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
@PreAuthorize("isAuthenticated()")
public class SmartIrrigationController {

    @Autowired
    private SmartIrrigationService smartIrrigationService;

    /**
     * GET /api/irrigation/recommendations/{cropId}
     * 
     * Get irrigation recommendations for a specific crop
     * 
     * @param cropId - Crop ID to get recommendations for
     * @param authentication - Current user context (from JWT)
     * @return IrrigationRecommendationDto with:
     *   - nextIrrigationDate: Recommended date
     *   - waterQuantityMm: Amount of water needed
     *   - intervalDays: Days between irrigations
     *   - lastIrrigationDate: When crop was last irrigated
     *   - urgencyLevel: 'OVERDUE', 'DUE_SOON', 'UPCOMING'
     * 
     * @throws ResourceNotFoundException if crop not found
     * @throws UnauthorizedException if user doesn't own the crop
     */
    @GetMapping("/recommendations/{cropId}")
    public ResponseEntity<ApiResponse<IrrigationRecommendationDto>> getRecommendations(
            @PathVariable Long cropId,
            Authentication authentication) {

        String userEmail = authentication.getName();

        try {
            IrrigationRecommendationDto recommendation = 
                smartIrrigationService.getIrrigationRecommendation(cropId, userEmail);
            
            return ResponseEntity.ok(new ApiResponse<>(
                "Irrigation recommendation fetched successfully",
                recommendation,
                null
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        }
    }

    /**
     * GET /api/irrigation/pending/{farmId}
     * 
     * Get all pending/overdue irrigations for a farm
     * 
     * @param farmId - Farm ID
     * @param authentication - Current user context
     * @return List of irrigation schedules with:
     *   - cropId, cropName
     *   - nextIrrigationDate
     *   - daysUntilIrrigation (negative if overdue)
     *   - urgencyStatus ('OVERDUE', 'DUE_SOON', 'UPCOMING')
     *   - waterQuantityMm
     */
    @GetMapping("/pending/{farmId}")
    public ResponseEntity<ApiResponse<List<IrrigationScheduleDto>>> getPendingIrrigations(
            @PathVariable Long farmId,
            @RequestParam(required = false, defaultValue = "7") int daysAhead,
            Authentication authentication) {

        String userEmail = authentication.getName();

        try {
            List<IrrigationScheduleDto> pending = 
                smartIrrigationService.getPendingIrrigations(farmId, userEmail, daysAhead);
            
            return ResponseEntity.ok(new ApiResponse<>(
                "Pending irrigations fetched successfully",
                pending,
                null
            ));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        }
    }

    /**
     * POST /api/irrigation/confirm/{scheduleId}
     * 
     * Confirm that farmer has irrigated the crop
     * Records the action and triggers next recommendation calculation
     * 
     * @param scheduleId - Schedule ID to confirm
     * @param request - ConfirmIrrigationRequest:
     *   - actualWaterUsedMm: How much water was actually used (optional)
     *   - notes: Farmer notes (optional)
     * 
     * @return Updated schedule and next recommendation
     * 
     * Example Request:
     * {
     *   "actualWaterUsedMm": 48.5,
     *   "notes": "Used drip irrigation, very efficient"
     * }
     */
    @PostMapping("/confirm/{scheduleId}")
    public ResponseEntity<ApiResponse<IrrigationScheduleDto>> confirmIrrigation(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ConfirmIrrigationRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        try {
            IrrigationScheduleDto confirmed = smartIrrigationService.confirmIrrigation(
                scheduleId, 
                userEmail,
                request.getActualWaterUsedMm(),
                request.getNotes()
            );
            
            return ResponseEntity.ok(new ApiResponse<>(
                "Irrigation confirmed successfully. Next recommendation calculated.",
                confirmed,
                null
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        }
    }

    /**
     * POST /api/irrigation/skip/{scheduleId}
     * 
     * Skip an irrigation (e.g., due to rain, postponement)
     * Records reason and calculates next recommendation
     * 
     * @param scheduleId - Schedule ID to skip
     * @param request - SkipIrrigationRequest:
     *   - reason: Why irrigation was skipped ('RAIN', 'DELAY', 'OTHER')
     *   - notes: Additional notes
     *   - rescheduleDate: If postponing, optional new date
     * 
     * @return Updated schedule with next recommendation
     * 
     * Example Request:
     * {
     *   "reason": "RAIN",
     *   "notes": "Unexpected rainfall during night",
     *   "rescheduleDate": "2025-07-08"
     * }
     */
    @PostMapping("/skip/{scheduleId}")
    public ResponseEntity<ApiResponse<IrrigationScheduleDto>> skipIrrigation(
            @PathVariable Long scheduleId,
            @Valid @RequestBody SkipIrrigationRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        try {
            IrrigationScheduleDto skipped = smartIrrigationService.skipIrrigation(
                scheduleId, 
                userEmail,
                request.getReason(),
                request.getNotes(),
                request.getRescheduleDate()
            );
            
            return ResponseEntity.ok(new ApiResponse<>(
                "Irrigation skipped. Next recommendation calculated.",
                skipped,
                null
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        }
    }

    /**
     * GET /api/irrigation/history/{farmId}
     * 
     * Get irrigation history for a farm (audit trail and analytics)
     * 
     * @param farmId - Farm ID
     * @param startDate - Optional filter (from date)
     * @param endDate - Optional filter (to date)
     * @param cropId - Optional filter (specific crop)
     * @param authentication - Current user context
     * 
     * @return List of historical irrigation events with:
     *   - plannedDate vs actualDate
     *   - plannedWater vs actualWater
     *   - waterEfficiencyPercentage
     *   - status (COMPLETED, SKIPPED, OVERDUE)
     * 
     * Example Response:
     * [
     *   {
     *     "id": 101,
     *     "cropId": 42,
     *     "cropName": "Rice",
     *     "plannedIrrigationDate": "2025-07-02",
     *     "actualIrrigationDate": "2025-07-04",
     *     "plannedWaterQuantityMm": 50.0,
     *     "actualWaterUsedMm": 48.5,
     *     "waterEfficiencyPercentage": 97.0,
     *     "status": "COMPLETED",
     *     "farmerNotes": "Used drip irrigation"
     *   }
     * ]
     */
    @GetMapping("/history/{farmId}")
    public ResponseEntity<ApiResponse<List<IrrigationHistoryDto>>> getIrrigationHistory(
            @PathVariable Long farmId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long cropId,
            Authentication authentication) {

        String userEmail = authentication.getName();

        try {
            List<IrrigationHistoryDto> history = smartIrrigationService.getIrrigationHistory(
                farmId,
                userEmail,
                startDate,
                endDate,
                cropId
            );
            
            return ResponseEntity.ok(new ApiResponse<>(
                "Irrigation history fetched successfully",
                history,
                null
            ));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        }
    }

    /**
     * GET /api/irrigation/statistics/{farmId}
     * 
     * Get farm irrigation statistics and efficiency metrics
     * 
     * @param farmId - Farm ID
     * @param season - Optional filter (KHARIF, RABI, SUMMER)
     * @param authentication - Current user context
     * 
     * @return Statistics:
     *   - totalIrrigationsMonth: How many times irrigated this month
     *   - averageWaterUsedMm: Average per irrigation
     *   - totalWaterUsedLiters: Total volume for period
     *   - waterSavedVsRecommended: Efficiency savings
     *   - efficiencyRating: 0-100%, >100% = waste
     *   - costSavedEstimated: ₹ savings
     * 
     * Example Response:
     * {
     *   "farmId": 5,
     *   "farmName": "Suresh's Field",
     *   "totalIrrigationsMonth": 4,
     *   "averageWaterUsedMm": 47.5,
     *   "totalWaterUsedLiters": 19000,
     *   "waterSavedVsRecommended": 8.5,
     *   "efficiencyRating": 95.0,
     *   "costSavedEstimated": 425.50,
     *   "season": "KHARIF",
     *   "periodStartDate": "2025-07-01",
     *   "periodEndDate": "2025-07-31"
     * }
     */
    @GetMapping("/statistics/{farmId}")
    public ResponseEntity<ApiResponse<Object>> getIrrigationStatistics(
            @PathVariable Long farmId,
            @RequestParam(required = false) String season,
            Authentication authentication) {

        String userEmail = authentication.getName();

        try {
            Object statistics = smartIrrigationService.getIrrigationStatistics(
                farmId,
                userEmail,
                season
            );
            
            return ResponseEntity.ok(new ApiResponse<>(
                "Irrigation statistics fetched successfully",
                statistics,
                null
            ));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        }
    }

    /**
     * POST /api/irrigation/toggle-reminders/{scheduleId}
     * 
     * Enable/disable irrigation reminders for a crop
     * 
     * @param scheduleId - Schedule ID
     * @param request - Toggle request:
     *   - reminderEnabled: true/false
     *   - reminderDaysBefore: Days before to send reminder (1-7)
     * 
     * @return Updated schedule
     */
    @PostMapping("/toggle-reminders/{scheduleId}")
    public ResponseEntity<ApiResponse<IrrigationScheduleDto>> toggleReminders(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ToggleRemindersRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        try {
            IrrigationScheduleDto updated = smartIrrigationService.toggleReminders(
                scheduleId,
                userEmail,
                request.getReminderEnabled(),
                request.getReminderDaysBefore()
            );
            
            return ResponseEntity.ok(new ApiResponse<>(
                "Reminders toggled successfully",
                updated,
                null
            ));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(e.getMessage(), null, null));
        }
    }

    /**
     * GET /api/irrigation/health
     * 
     * System health check (no auth required)
     * Used for monitoring
     * 
     * @return Health status and metrics
     */
    @GetMapping("/health")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<Object>> healthCheck() {
        try {
            Object health = smartIrrigationService.getSystemHealth();
            return ResponseEntity.ok(new ApiResponse<>(
                "Smart Irrigation System is healthy",
                health,
                null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(
                    "Smart Irrigation System health check failed: " + e.getMessage(),
                    null,
                    null
                ));
        }
    }

    // ========== DTO CLASSES ==========

    /**
     * Request body for confirming irrigation
     */
    public static class ConfirmIrrigationRequest {
        private Double actualWaterUsedMm;
        private String notes;

        public ConfirmIrrigationRequest() {}

        public Double getActualWaterUsedMm() {
            return actualWaterUsedMm;
        }

        public void setActualWaterUsedMm(Double actualWaterUsedMm) {
            this.actualWaterUsedMm = actualWaterUsedMm;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Request body for skipping irrigation
     */
    public static class SkipIrrigationRequest {
        private String reason;  // RAIN, DELAY, OTHER
        private String notes;
        private LocalDate rescheduleDate;

        public SkipIrrigationRequest() {}

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public LocalDate getRescheduleDate() {
            return rescheduleDate;
        }

        public void setRescheduleDate(LocalDate rescheduleDate) {
            this.rescheduleDate = rescheduleDate;
        }
    }

    /**
     * Request body for toggling reminders
     */
    public static class ToggleRemindersRequest {
        private Boolean reminderEnabled;
        private Integer reminderDaysBefore;

        public ToggleRemindersRequest() {}

        public Boolean getReminderEnabled() {
            return reminderEnabled;
        }

        public void setReminderEnabled(Boolean reminderEnabled) {
            this.reminderEnabled = reminderEnabled;
        }

        public Integer getReminderDaysBefore() {
            return reminderDaysBefore;
        }

        public void setReminderDaysBefore(Integer reminderDaysBefore) {
            this.reminderDaysBefore = reminderDaysBefore;
        }
    }
}

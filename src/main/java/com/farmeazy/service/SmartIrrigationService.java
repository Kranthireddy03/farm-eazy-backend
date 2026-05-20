package com.farmeazy.service;

import com.farmeazy.dto.IrrigationRecommendationDto;
import com.farmeazy.dto.IrrigationScheduleDto;
import com.farmeazy.dto.IrrigationHistoryDto;
import com.farmeazy.entity.*;
import com.farmeazy.repository.*;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Irrigation Service - Core business logic for irrigation recommendations
 * 
 * Key Features:
 * 1. Calculate next irrigation date based on rules
 * 2. Send reminders (SMS, Email, Push)
 * 3. Track farmer actions (confirm/skip)
 * 4. Calculate efficiency metrics
 * 5. Provide optimization suggestions
 */
@Service
@Transactional
public class SmartIrrigationService {

    private static final Logger logger = LoggerFactory.getLogger(SmartIrrigationService.class);
    private static final String PILOT_LOCATION_KEYWORD = "ANANTHAPUR";
    private static final Set<String> PILOT_CROPS = Set.of(
        "GROUNDNUT", "SUNFLOWER", "MAIZE", "COTTON", "PADDY", "MILLET"
    );

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private IrrigationScheduleRepository irrigationScheduleRepository;

    @Autowired
    private CropIrrigationRuleRepository cropIrrigationRuleRepository;

    @Autowired
    private IrrigationHistoryRepository irrigationHistoryRepository;

    @Autowired
    private IrrigationRemindersLogRepository irrigationRemindersLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Get irrigation recommendation for a specific crop
     * 
     * Algorithm:
     * 1. Get crop details + farm soil type
     * 2. Determine current season
     * 3. Look up rule: crop + soil + season + region
     * 4. Get last irrigation date
     * 5. Calculate next_date = last + interval_days
     * 6. Return recommendation with urgency
     */
    public IrrigationRecommendationDto getIrrigationRecommendation(Long cropId, String userEmail) {
        logger.info("Fetching irrigation recommendation for crop {} by user {}", cropId, userEmail);

        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Crop crop = cropRepository.findByIdAndFarmUserId(cropId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Crop not found or not owned by user: " + cropId));

        Farm farm = crop.getFarm();
        validatePilotScope(farm, crop);

        // Get or create irrigation schedule
        IrrigationSchedule schedule = irrigationScheduleRepository.findByCropId(cropId)
            .stream()
            .findFirst()
            .orElseGet(() -> createNewIrrigationSchedule(crop, farm));

        // Build recommendation
        IrrigationRecommendationDto recommendation = new IrrigationRecommendationDto();
        recommendation.setCropId(cropId);
        recommendation.setCropName(crop.getCropName());
        recommendation.setNextIrrigationDate(schedule.getNextIrrigationDate());
        recommendation.setWaterQuantityMm(schedule.getRecommendedWaterQuantityMm());
        recommendation.setIntervalDays(schedule.getIntervalDays());
        recommendation.setLastIrrigationDate(schedule.getLastIrrigationDate());
        recommendation.setUrgencyLevel(calculateUrgency(schedule.getNextIrrigationDate()));

        logger.info("Recommendation calculated: next date {}, water {} mm", 
            recommendation.getNextIrrigationDate(),
            recommendation.getWaterQuantityMm());

        return recommendation;
    }

    /**
     * Get all pending irrigations for a farm (next N days)
     */
    public List<IrrigationScheduleDto> getPendingIrrigations(Long farmId, String userEmail, int daysAhead) {
        logger.info("Fetching pending irrigations for farm {} (next {} days)", farmId, daysAhead);

        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Farm farm = farmRepository.findByIdAndUserId(farmId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Farm not found or not owned by user"));
        validatePilotLocation(farm);

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead);

        // Get all schedules due in next N days
        List<IrrigationSchedule> schedules = irrigationScheduleRepository
            .findDueIrrigations(farmId, today, endDate);

        return schedules.stream()
            .map(this::toScheduleDto)
            .collect(Collectors.toList());
    }

    /**
     * Confirm that farmer has irrigated the crop
     * 
     * Actions:
     * 1. Record in irrigation_history
     * 2. Calculate next irrigation date
     * 3. Update irrigation_schedule
     * 4. If using less water than recommended: track efficiency
     */
    public IrrigationScheduleDto confirmIrrigation(
            Long scheduleId,
            String userEmail,
            Double actualWaterUsedMm,
            String notes) {

        logger.info("Confirming irrigation for schedule {} by user {}", scheduleId, userEmail);

        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        IrrigationSchedule schedule = irrigationScheduleRepository.findByIdAndFarmUserId(scheduleId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        validatePilotScope(schedule.getFarm(), schedule.getCrop());

        // Record in history
        IrrigationHistory history = new IrrigationHistory();
        history.setUserId(user.getId());
        history.setFarmId(schedule.getFarm().getId());
        history.setCropId(schedule.getCrop().getId());
        history.setPlannedIrrigationDate(schedule.getNextIrrigationDate());
        history.setActualIrrigationDate(LocalDate.now());
        history.setPlannedWaterQuantityMm(schedule.getRecommendedWaterQuantityMm());
        history.setActualWaterUsedMm(actualWaterUsedMm != null ? 
            actualWaterUsedMm : schedule.getRecommendedWaterQuantityMm());
        history.setStatus("COMPLETED");
        history.setFarmerNotes(notes);

        // Calculate efficiency
        if (actualWaterUsedMm != null && schedule.getRecommendedWaterQuantityMm() != null) {
            double efficiency = (actualWaterUsedMm / schedule.getRecommendedWaterQuantityMm()) * 100;
            history.setWaterEfficiencyPercentage(efficiency);
            logger.info("Water efficiency: {} %", efficiency);
        }

        irrigationHistoryRepository.save(history);

        // Update schedule
        schedule.setLastIrrigationDate(LocalDate.now());
        schedule.setLastIrrigationQuantityMm(actualWaterUsedMm);
        schedule.setStatus("SCHEDULED");

        // Calculate next irrigation
        LocalDate nextDate = LocalDate.now().plusDays(schedule.getIntervalDays());
        schedule.setNextIrrigationDate(nextDate);

        IrrigationSchedule updated = irrigationScheduleRepository.save(schedule);

        logger.info("Irrigation confirmed. Next scheduled for {}", nextDate);
        return toScheduleDto(updated);
    }

    /**
     * Skip an irrigation
     * 
     * Reasons:
     * - RAIN: Natural rainfall sufficient
     * - DELAY: Postponing temporarily
     * - OTHER: Custom reason
     */
    public IrrigationScheduleDto skipIrrigation(
            Long scheduleId,
            String userEmail,
            String reason,
            String notes,
            LocalDate rescheduleDate) {

        logger.info("Skipping irrigation for schedule {} - Reason: {}", scheduleId, reason);

        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        IrrigationSchedule schedule = irrigationScheduleRepository.findByIdAndFarmUserId(scheduleId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        validatePilotScope(schedule.getFarm(), schedule.getCrop());

        // Record in history
        IrrigationHistory history = new IrrigationHistory();
        history.setUserId(user.getId());
        history.setFarmId(schedule.getFarm().getId());
        history.setCropId(schedule.getCrop().getId());
        history.setPlannedIrrigationDate(schedule.getNextIrrigationDate());
        history.setActualIrrigationDate(LocalDate.now()); // When skipped
        history.setStatus("SKIPPED");
        history.setReasonForSkip(reason);
        history.setFarmerNotes(notes);

        irrigationHistoryRepository.save(history);

        // Update schedule
        if (rescheduleDate != null && rescheduleDate.isAfter(LocalDate.now())) {
            schedule.setNextIrrigationDate(rescheduleDate);
            logger.info("Irrigation rescheduled for {}", rescheduleDate);
        } else {
            // Default: add interval to today
            schedule.setNextIrrigationDate(
                LocalDate.now().plusDays(schedule.getIntervalDays())
            );
        }

        schedule.setStatus("SCHEDULED");
        IrrigationSchedule updated = irrigationScheduleRepository.save(schedule);

        return toScheduleDto(updated);
    }

    /**
     * Get irrigation history for a farm
     * 
     * Used for:
     * - Analytics dashboard
     * - Efficiency tracking
     * - Farmer insights
     */
    public List<IrrigationHistoryDto> getIrrigationHistory(
            Long farmId,
            String userEmail,
            LocalDate startDate,
            LocalDate endDate,
            Long cropId) {

        logger.info("Fetching irrigation history for farm {}", farmId);

        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Farm farm = farmRepository.findByIdAndUserId(farmId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Farm not found or not owned by user"));
        validatePilotLocation(farm);

        LocalDate from = startDate != null ? startDate : LocalDate.now().minusMonths(6);
        LocalDate to = endDate != null ? endDate : LocalDate.now();

        List<IrrigationHistory> histories;
        if (cropId != null) {
            histories = irrigationHistoryRepository
                .findByFarmIdAndCropIdAndDateRange(farmId, cropId, from, to);
        } else {
            histories = irrigationHistoryRepository
                .findByFarmIdAndDateRange(farmId, from, to);
        }

        return histories.stream()
            .map(this::toHistoryDto)
            .collect(Collectors.toList());
    }

    /**
     * Get farm irrigation statistics
     * 
     * Metrics include:
     * - Total irrigations this month
     * - Average water per irrigation
     * - Water efficiency rating (0-100%)
     * - Cost savings
     */
    public Object getIrrigationStatistics(Long farmId, String userEmail, String season) {
        logger.info("Calculating irrigation statistics for farm {}", farmId);

        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Farm farm = farmRepository.findByIdAndUserId(farmId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Farm not found or not owned by user"));
        validatePilotLocation(farm);

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        List<IrrigationHistory> monthlyHistory = 
            irrigationHistoryRepository.findByFarmIdAndDateRange(
                farmId, monthStart, monthEnd
            );

        // Calculate metrics
        Map<String, Object> stats = new HashMap<>();
        stats.put("farmId", farmId);
        stats.put("farmName", farm.getFarmName());
        stats.put("totalIrrigationsMonth", monthlyHistory.size());
        
        if (!monthlyHistory.isEmpty()) {
            double avgWater = monthlyHistory.stream()
                .mapToDouble(h -> h.getActualWaterUsedMm() != null ? 
                    h.getActualWaterUsedMm() : 0)
                .average()
                .orElse(0);
            
            double totalWaterLiters = monthlyHistory.stream()
                .mapToDouble(h -> h.getActualWaterUsedMm() != null ? 
                    h.getActualWaterUsedMm() * farm.getAreaSize() * 10000 : 0)
                .sum();
            
            double avgEfficiency = monthlyHistory.stream()
                .mapToDouble(h -> h.getWaterEfficiencyPercentage() != null ? 
                    h.getWaterEfficiencyPercentage() : 100)
                .average()
                .orElse(100);

            stats.put("averageWaterUsedMm", Math.round(avgWater * 100.0) / 100.0);
            stats.put("totalWaterUsedLiters", Math.round(totalWaterLiters * 100.0) / 100.0);
            stats.put("efficiencyRating", Math.round(avgEfficiency * 10.0) / 10.0);
            
            // Rough cost estimate: ₹5 per 1000 liters pump, ₹2.5 per 1000 liters water
            double estimatedCost = (totalWaterLiters / 1000) * 7.5;
            double savedCost = estimatedCost * (100 - avgEfficiency) / 100;
            stats.put("costSavedEstimated", Math.round(savedCost * 100.0) / 100.0);
        }

        stats.put("periodStartDate", monthStart);
        stats.put("periodEndDate", monthEnd);
        stats.put("season", season != null ? season : getCurrentSeason());

        logger.info("Statistics calculated: {} irrigations, {} % efficiency", 
            stats.get("totalIrrigationsMonth"),
            stats.get("efficiencyRating"));

        return stats;
    }

    /**
     * Toggle reminders for a crop
     */
    public IrrigationScheduleDto toggleReminders(
            Long scheduleId,
            String userEmail,
            Boolean reminderEnabled,
            Integer reminderDaysBefore) {

        logger.info("Toggling reminders for schedule {}", scheduleId);

        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        IrrigationSchedule schedule = irrigationScheduleRepository.findByIdAndFarmUserId(scheduleId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));

        if (reminderEnabled != null) {
            schedule.setReminderEnabled(reminderEnabled);
        }

        if (reminderDaysBefore != null && reminderDaysBefore > 0 && reminderDaysBefore <= 7) {
            schedule.setReminderDaysBefore(reminderDaysBefore);
        }

        IrrigationSchedule updated = irrigationScheduleRepository.save(schedule);
        return toScheduleDto(updated);
    }

    /**
     * Get system health status (for monitoring)
     */
    public Object getSystemHealth() {
        try {
            long totalRules = cropIrrigationRuleRepository.count();
            long totalSchedules = irrigationScheduleRepository.count();
            long pendingReminders = irrigationRemindersLogRepository
                .countByStatus("FAILED");

            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("totalRules", totalRules);
            health.put("totalSchedules", totalSchedules);
            health.put("failedReminders", pendingReminders);
            health.put("message", "Smart Irrigation System is operational");

            return health;
        } catch (Exception e) {
            logger.error("Health check failed", e);
            throw new RuntimeException("System health check failed");
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Create new irrigation schedule for a crop
     */
    private IrrigationSchedule createNewIrrigationSchedule(Crop crop, Farm farm) {
        logger.info("Creating new irrigation schedule for crop {}", crop.getId());

        IrrigationSchedule schedule = new IrrigationSchedule();
        schedule.setCrop(crop);
        schedule.setFarm(farm);
        schedule.setUserId(farm.getUser().getId());
        schedule.setStatus("SCHEDULED");
        schedule.setReminderEnabled(true);
        schedule.setReminderDaysBefore(1);
        schedule.setActive(true);

        // Get irrigation rule
        CropIrrigationRule rule = findApplicableRule(crop, farm);
        if (rule != null) {
            schedule.setIntervalDays(rule.getIrrigationIntervalDays());
            schedule.setRecommendedWaterQuantityMm(rule.getWaterQuantityMm());
            schedule.setNextIrrigationDate(LocalDate.now().plusDays(rule.getIrrigationIntervalDays()));
        } else {
            // Fallback: 5 days, 40mm
            schedule.setIntervalDays(5);
            schedule.setRecommendedWaterQuantityMm(40.0);
            schedule.setNextIrrigationDate(LocalDate.now().plusDays(5));
        }

        schedule.setLastIrrigationDate(crop.getSowingDate());
        schedule.setIrrigationDate(schedule.getNextIrrigationDate());
        schedule.setStartTime(java.time.LocalTime.of(6, 0));
        schedule.setDuration(60);
        schedule.setWaterAmount(schedule.getRecommendedWaterQuantityMm());

        return irrigationScheduleRepository.save(schedule);
    }

    /**
     * Find applicable irrigation rule for a crop
     * 
     * Search order:
     * 1. Exact match: crop + soil + season + region
     * 2. Partial match: crop + soil + season
     * 3. Crop type default
     */
    private CropIrrigationRule findApplicableRule(Crop crop, Farm farm) {
        String season = getCurrentSeason();
        String cropName = crop.getCropName().toUpperCase();
        String soilType = farm.getSoilType() != null ? farm.getSoilType().toUpperCase() : null;
        String region = farm.getLocation() != null ? farm.getLocation().toUpperCase() : "KARNATAKA";

        // Try exact match first
        Optional<CropIrrigationRule> exactRule = cropIrrigationRuleRepository
            .findByExactMatch(cropName, soilType, season, region);
        if (exactRule.isPresent()) {
            return exactRule.get();
        }

        // Try crop + soil + season
        List<CropIrrigationRule> rules = cropIrrigationRuleRepository
            .findByVariant(cropName, soilType, season);
        if (!rules.isEmpty()) {
            return rules.get(0);
        }

        // Try crop type default
        rules = cropIrrigationRuleRepository.findByCropType(cropName);
        if (!rules.isEmpty()) {
            return rules.get(0);
        }

        return null;
    }

    /**
     * Calculate urgency level (for UI)
     */
    private String calculateUrgency(LocalDate nextDate) {
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), nextDate);
        
        if (daysUntil < 0) {
            return "OVERDUE";
        } else if (daysUntil <= 1) {
            return "DUE_SOON";
        } else {
            return "UPCOMING";
        }
    }

    /**
     * Determine current season in India
     */
    private String getCurrentSeason() {
        int month = LocalDate.now().getMonthValue();
        if (month >= 6 && month <= 10) {
            return "KHARIF";  // June-October
        } else if (month >= 10 || month <= 3) {
            return "RABI";    // October-March
        } else {
            return "SUMMER";  // April-May
        }
    }

    private void validatePilotScope(Farm farm, Crop crop) {
        validatePilotLocation(farm);
        String cropName = crop.getCropName() != null ? crop.getCropName().trim().toUpperCase() : "";
        if (!PILOT_CROPS.contains(cropName)) {
            throw new ResourceNotFoundException(
                "Smart irrigation pilot currently supports only " + PILOT_CROPS + " crops"
            );
        }
    }

    private void validatePilotLocation(Farm farm) {
        String location = farm.getLocation() != null ? farm.getLocation().trim().toUpperCase() : "";
        if (!location.contains(PILOT_LOCATION_KEYWORD)) {
            throw new ResourceNotFoundException(
                "Smart irrigation pilot is currently enabled only for Ananthapur, Andhra Pradesh"
            );
        }
    }

    // ========== DTO CONVERSION METHODS ==========

    private IrrigationScheduleDto toScheduleDto(IrrigationSchedule schedule) {
        IrrigationScheduleDto dto = new IrrigationScheduleDto();
        dto.setId(schedule.getId());
        dto.setCropId(schedule.getCrop().getId());
        dto.setCropName(schedule.getCrop().getCropName());
        dto.setNextIrrigationDate(schedule.getNextIrrigationDate());
        dto.setRecommendedWaterQuantityMm(schedule.getRecommendedWaterQuantityMm());
        dto.setIntervalDays(schedule.getIntervalDays());
        dto.setLastIrrigationDate(schedule.getLastIrrigationDate());
        dto.setStatus(schedule.getStatus());
        dto.setReminderEnabled(schedule.getReminderEnabled());
        
        if (schedule.getNextIrrigationDate() != null) {
            long daysUntil = java.time.temporal.ChronoUnit.DAYS
                .between(LocalDate.now(), schedule.getNextIrrigationDate());
            dto.setDaysUntilIrrigation((int) daysUntil);
            dto.setUrgencyStatus(calculateUrgency(schedule.getNextIrrigationDate()));
        }

        return dto;
    }

    private IrrigationHistoryDto toHistoryDto(IrrigationHistory history) {
        IrrigationHistoryDto dto = new IrrigationHistoryDto();
        dto.setId(history.getId());
        dto.setCropId(history.getCropId());
        dto.setPlannedIrrigationDate(history.getPlannedIrrigationDate());
        dto.setActualIrrigationDate(history.getActualIrrigationDate());
        dto.setPlannedWaterQuantityMm(history.getPlannedWaterQuantityMm());
        dto.setActualWaterUsedMm(history.getActualWaterUsedMm());
        dto.setWaterEfficiencyPercentage(history.getWaterEfficiencyPercentage());
        dto.setStatus(history.getStatus());
        dto.setFarmerNotes(history.getFarmerNotes());

        return dto;
    }
}

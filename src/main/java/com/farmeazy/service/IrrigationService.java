package com.farmeazy.service;

import com.farmeazy.dto.DashboardStatsDto;
import com.farmeazy.dto.IrrigationScheduleDto;
import com.farmeazy.entity.Crop;
import com.farmeazy.entity.Farm;
import com.farmeazy.entity.IrrigationSchedule;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.CropRepository;
import com.farmeazy.repository.FarmRepository;
import com.farmeazy.repository.IrrigationScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IrrigationService {

    private static final Logger logger = LoggerFactory.getLogger(IrrigationService.class);

    @Autowired
    private IrrigationScheduleRepository irrigationRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private HttpEmailService httpEmailService;

    @Autowired
    private SmsService smsService;

    @Transactional
    public IrrigationScheduleDto createSchedule(IrrigationScheduleDto scheduleDto, Long userId) {
        logger.info("IRRIGATION_SERVICE_CREATE_START userId={} farmId={} cropId={} irrigationDate={}", userId, scheduleDto != null ? scheduleDto.getFarmId() : null, scheduleDto != null ? scheduleDto.getCropId() : null, scheduleDto != null ? scheduleDto.getIrrigationDate() : null);
        Crop crop = cropRepository.findById(scheduleDto.getCropId())
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));
        
        Farm farm = farmRepository.findById(scheduleDto.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));

        if (crop.getFarm() == null || !crop.getFarm().getId().equals(farm.getId())) {
            throw new UnauthorizedException("Selected crop does not belong to the provided farm");
        }
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to create schedules for this farm");
        }

        if (!crop.getFarm().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to create schedules for this crop");
        }

        IrrigationSchedule schedule = new IrrigationSchedule();
        schedule.setIrrigationDate(scheduleDto.getIrrigationDate());
        schedule.setStartTime(scheduleDto.getStartTime());
        schedule.setDuration(scheduleDto.getDuration());
        schedule.setWaterAmount(scheduleDto.getWaterAmount());
        schedule.setStatus(scheduleDto.getStatus() != null ? scheduleDto.getStatus() : "SCHEDULED");
        schedule.setNotes(scheduleDto.getNotes());
        schedule.setCrop(crop);
        schedule.setFarm(farm);

        schedule = irrigationRepository.save(schedule);
        
        // Log activity
        userActivityService.logActivity(farm.getUser(), com.farmeazy.entity.UserActivity.ActivityType.IRRIGATION_SCHEDULED, "Scheduled irrigation for crop '" + crop.getCropName() + "' on " + schedule.getIrrigationDate());
        
        // Send reminder email using HTTP service (works on Render)
        try {
            String message = "Irrigation schedule has been created for crop '" + crop.getCropName() + "' on your farm '" + farm.getFarmName() + "'. "
                    + "Date: " + schedule.getIrrigationDate() + ", Start Time: " + schedule.getStartTime() 
                    + ", Duration: " + schedule.getDuration() + " minutes, Water Amount: " + schedule.getWaterAmount() + " liters.";
            httpEmailService.sendNotification(farm.getUser().getEmail(), farm.getUser().getUsername(),
                "Irrigation Schedule Created - FarmEazy", message);
        } catch (Exception e) {
            logger.warn("IRRIGATION_SERVICE_CREATE_EMAIL_FAILED userId={} scheduleId={} message={}", userId, schedule.getId(), e.getMessage());
        }
        
        // Send SMS notification for irrigation reminder
        try {
            String userPhone = farm.getUser().getPhone();
            if (userPhone != null && !userPhone.isBlank()) {
                smsService.sendIrrigationReminder(
                    userPhone,
                    crop.getCropName(),
                    farm.getFarmName()
                );
            }
        } catch (Exception smsEx) {
            logger.warn("IRRIGATION_SERVICE_CREATE_SMS_FAILED userId={} scheduleId={} message={}", userId, schedule.getId(), smsEx.getMessage());
        }

        logger.info("IRRIGATION_SERVICE_CREATE_SUCCESS userId={} scheduleId={}", userId, schedule.getId());
        
        return mapScheduleToDto(schedule, userId);
    }

    public IrrigationScheduleDto getScheduleById(Long scheduleId, Long userId) {
        logger.info("IRRIGATION_SERVICE_GET_BY_ID scheduleId={} userId={}", scheduleId, userId);
        IrrigationSchedule schedule = irrigationRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        
        if (!schedule.getFarm().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this schedule");
        }

        return mapScheduleToDto(schedule, userId);
    }

    public Page<IrrigationScheduleDto> getSchedulesByFarm(Long farmId, Long userId, Pageable pageable) {
        logger.info("IRRIGATION_SERVICE_GET_BY_FARM farmId={} userId={} page={} size={}", farmId, userId, pageable.getPageNumber(), pageable.getPageSize());
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this farm");
        }

        return irrigationRepository.findByFarmId(farmId, pageable)
                .map(schedule -> mapScheduleToDto(schedule, userId));
    }

    public List<IrrigationScheduleDto> getUpcomingSchedules(Long userId) {
        logger.info("IRRIGATION_SERVICE_GET_UPCOMING userId={}", userId);
        List<Farm> farms = farmRepository.findByUserId(userId);
        List<Long> farmIds = farms.stream().map(Farm::getId).toList();
        
        return farmIds.stream()
                .flatMap(farmId -> irrigationRepository.findByIrrigationDateAfter(LocalDate.now()).stream()
                        .filter(s -> s.getFarm().getId().equals(farmId)))
                .map(schedule -> mapScheduleToDto(schedule, userId))
                .toList();
    }

    @Transactional
    public IrrigationScheduleDto updateSchedule(Long scheduleId, IrrigationScheduleDto scheduleDto, Long userId) {
        logger.info("IRRIGATION_SERVICE_UPDATE_START scheduleId={} userId={} irrigationDate={}", scheduleId, userId, scheduleDto != null ? scheduleDto.getIrrigationDate() : null);
        IrrigationSchedule schedule = irrigationRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        
        if (!schedule.getFarm().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to update this schedule");
        }

        schedule.setIrrigationDate(scheduleDto.getIrrigationDate());
        schedule.setStartTime(scheduleDto.getStartTime());
        schedule.setDuration(scheduleDto.getDuration());
        schedule.setWaterAmount(scheduleDto.getWaterAmount());
        schedule.setNotes(scheduleDto.getNotes());

        schedule = irrigationRepository.save(schedule);
        
        // Log activity
        userActivityService.logActivity(schedule.getFarm().getUser(), com.farmeazy.entity.UserActivity.ActivityType.IRRIGATION_UPDATED, "Updated irrigation schedule for crop '" + schedule.getCrop().getCropName() + "'");

        // Send update email notification
        try {
            String message = "Irrigation schedule for crop '" + schedule.getCrop().getCropName() + "' on your farm '" + schedule.getFarm().getFarmName() + "' has been updated. "
                    + "Date: " + schedule.getIrrigationDate() + ", Start Time: " + schedule.getStartTime()
                    + ", Duration: " + schedule.getDuration() + " minutes, Water Amount: " + schedule.getWaterAmount() + " liters.";
            httpEmailService.sendNotification(schedule.getFarm().getUser().getEmail(), schedule.getFarm().getUser().getUsername(),
                "Irrigation Schedule Updated - FarmEazy", message);
        } catch (Exception e) {
            logger.warn("IRRIGATION_SERVICE_UPDATE_EMAIL_FAILED scheduleId={} userId={} message={}", scheduleId, userId, e.getMessage());
        }

        logger.info("IRRIGATION_SERVICE_UPDATE_SUCCESS scheduleId={} userId={}", scheduleId, userId);
        
        return mapScheduleToDto(schedule, userId);
    }

    @Transactional
    public IrrigationScheduleDto markAsCompleted(Long scheduleId, Long userId) {
        logger.info("IRRIGATION_SERVICE_MARK_COMPLETED_START scheduleId={} userId={}", scheduleId, userId);
        IrrigationSchedule schedule = irrigationRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        
        if (!schedule.getFarm().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to update this schedule");
        }

        schedule.setStatus("COMPLETED");
        schedule.setCompletedAt(LocalDateTime.now());
        schedule = irrigationRepository.save(schedule);
        
        // Log activity
        userActivityService.logActivity(schedule.getFarm().getUser(), com.farmeazy.entity.UserActivity.ActivityType.IRRIGATION_COMPLETED, "Marked irrigation as completed for crop '" + schedule.getCrop().getCropName() + "'");

        logger.info("IRRIGATION_SERVICE_MARK_COMPLETED_SUCCESS scheduleId={} userId={}", scheduleId, userId);
        
        return mapScheduleToDto(schedule, userId);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId) {
        logger.info("IRRIGATION_SERVICE_DELETE_START scheduleId={} userId={}", scheduleId, userId);
        IrrigationSchedule schedule = irrigationRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        
        if (!schedule.getFarm().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to delete this schedule");
        }

        // Log activity before deleting
        userActivityService.logActivity(schedule.getFarm().getUser(), com.farmeazy.entity.UserActivity.ActivityType.IRRIGATION_DELETED, "Deleted irrigation schedule for crop '" + schedule.getCrop().getCropName() + "'");

        // Send delete email notification
        try {
            String message = "Irrigation schedule for crop '" + schedule.getCrop().getCropName() + "' on your farm '" + schedule.getFarm().getFarmName() + "' has been deleted.";
            httpEmailService.sendNotification(schedule.getFarm().getUser().getEmail(), schedule.getFarm().getUser().getUsername(),
                "Irrigation Schedule Deleted - FarmEazy", message);
        } catch (Exception e) {
            logger.warn("IRRIGATION_SERVICE_DELETE_EMAIL_FAILED scheduleId={} userId={} message={}", scheduleId, userId, e.getMessage());
        }

        irrigationRepository.delete(schedule);
        logger.info("IRRIGATION_SERVICE_DELETE_SUCCESS scheduleId={} userId={}", scheduleId, userId);
    }

    public List<IrrigationScheduleDto> getAllSchedulesByUser(Long userId) {
        logger.info("IRRIGATION_SERVICE_GET_ALL_BY_USER userId={}", userId);
        List<Farm> farms = farmRepository.findByUserId(userId);
        List<Long> farmIds = farms.stream().map(Farm::getId).toList();
        
        return farmIds.stream()
                .flatMap(farmId -> irrigationRepository.findByFarmId(farmId).stream())
                .map(schedule -> mapScheduleToDto(schedule, userId))
                .toList();
    }

    public DashboardStatsDto getDashboardStats(Long userId) {
        logger.info("IRRIGATION_SERVICE_DASHBOARD_STATS userId={}", userId);
        List<Farm> farms = farmRepository.findByUserId(userId);
        
            int totalFarms = farms.size();
        
            int totalCrops = (int) farms.stream()
                .flatMap(farm -> cropRepository.findByFarmId(farm.getId()).stream())
                .count();
        
            int totalIrrigations = (int) farms.stream()
                .flatMap(farm -> irrigationRepository.findByFarmId(farm.getId()).stream())
                .count();
        
            int upcomingIrrigations = (int) farms.stream()
                .flatMap(farm -> irrigationRepository.findByIrrigationDateAfter(LocalDate.now()).stream()
                    .filter(s -> s.getFarm().getId().equals(farm.getId())))
                .count();

        return new DashboardStatsDto(totalFarms, totalCrops, totalIrrigations, upcomingIrrigations);
    }

    private IrrigationScheduleDto mapScheduleToDto(IrrigationSchedule schedule, Long userId) {
        IrrigationScheduleDto dto = new IrrigationScheduleDto();
        dto.setId(schedule.getId());
        dto.setIrrigationDate(schedule.getIrrigationDate());
        dto.setStartTime(schedule.getStartTime());
        dto.setDuration(schedule.getDuration());
        dto.setWaterAmount(schedule.getWaterAmount());
        dto.setStatus(schedule.getStatus());
        dto.setNotes(schedule.getNotes());
        dto.setActualWaterUsed(schedule.getActualWaterUsed());
        dto.setCropId(schedule.getCrop().getId());
        dto.setFarmId(schedule.getFarm().getId());
        dto.setFarmUserId(userId);
        return dto;
    }
}

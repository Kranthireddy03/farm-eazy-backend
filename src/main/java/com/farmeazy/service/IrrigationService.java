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

    @Transactional
    public IrrigationScheduleDto createSchedule(IrrigationScheduleDto scheduleDto, Long userId) {
        Crop crop = cropRepository.findById(scheduleDto.getCropId())
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));
        
        Farm farm = farmRepository.findById(scheduleDto.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to create schedules for this farm");
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
            httpEmailService.sendNotification(farm.getUser().getEmail(), farm.getUser().getFullName(),
                "Irrigation Schedule Created - FarmEazy", message);
        } catch (Exception e) {
            System.err.println("Failed to send irrigation schedule email: " + e.getMessage());
        }
        
        return mapScheduleToDto(schedule, userId);
    }

    public IrrigationScheduleDto getScheduleById(Long scheduleId, Long userId) {
        IrrigationSchedule schedule = irrigationRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        
        if (!schedule.getFarm().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this schedule");
        }

        return mapScheduleToDto(schedule, userId);
    }

    public Page<IrrigationScheduleDto> getSchedulesByFarm(Long farmId, Long userId, Pageable pageable) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this farm");
        }

        return irrigationRepository.findByFarmId(farmId, pageable)
                .map(schedule -> mapScheduleToDto(schedule, userId));
    }

    public List<IrrigationScheduleDto> getUpcomingSchedules(Long userId) {
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
            httpEmailService.sendNotification(schedule.getFarm().getUser().getEmail(), schedule.getFarm().getUser().getFullName(),
                "Irrigation Schedule Updated - FarmEazy", message);
        } catch (Exception e) {
            System.err.println("Failed to send irrigation update email: " + e.getMessage());
        }
        
        return mapScheduleToDto(schedule, userId);
    }

    @Transactional
    public IrrigationScheduleDto markAsCompleted(Long scheduleId, Long userId) {
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
        
        return mapScheduleToDto(schedule, userId);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId) {
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
            httpEmailService.sendNotification(schedule.getFarm().getUser().getEmail(), schedule.getFarm().getUser().getFullName(),
                "Irrigation Schedule Deleted - FarmEazy", message);
        } catch (Exception e) {
            System.err.println("Failed to send irrigation deletion email: " + e.getMessage());
        }

        irrigationRepository.delete(schedule);
    }

    public List<IrrigationScheduleDto> getAllSchedulesByUser(Long userId) {
        List<Farm> farms = farmRepository.findByUserId(userId);
        List<Long> farmIds = farms.stream().map(Farm::getId).toList();
        
        return farmIds.stream()
                .flatMap(farmId -> irrigationRepository.findByFarmId(farmId).stream())
                .map(schedule -> mapScheduleToDto(schedule, userId))
                .toList();
    }

    public DashboardStatsDto getDashboardStats(Long userId) {
        List<Farm> farms = farmRepository.findByUserId(userId);
        
        long totalFarms = farms.size();
        
        long totalCrops = farms.stream()
                .flatMap(farm -> cropRepository.findByFarmId(farm.getId()).stream())
                .count();
        
        long totalIrrigations = farms.stream()
                .flatMap(farm -> irrigationRepository.findByFarmId(farm.getId()).stream())
                .count();
        
        long upcomingIrrigations = farms.stream()
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

package com.farmeazy.service;

import com.farmeazy.dto.CropDto;
import com.farmeazy.entity.Crop;
import com.farmeazy.entity.Farm;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.CropRepository;
import com.farmeazy.repository.FarmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CropService {

    private static final Logger logger = LoggerFactory.getLogger(CropService.class);

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private HttpEmailService httpEmailService;

    @Autowired
    private NotificationService notificationService;

    @Transactional
        @Caching(evict = {
            @CacheEvict(cacheNames = "cropById", allEntries = true),
            @CacheEvict(cacheNames = "cropListByUser", key = "#userId"),
            @CacheEvict(cacheNames = "cropListByFarm", key = "#cropDto.farmId + ':' + #userId")
        })
    public CropDto createCrop(CropDto cropDto, Long userId) {
        logger.info("CROP_SERVICE_CREATE_START userId={} farmId={} cropName={}", userId, cropDto != null ? cropDto.getFarmId() : null, cropDto != null ? cropDto.getCropName() : null);
        Farm farm = farmRepository.findByIdAndUserId(cropDto.getFarmId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found or not owned by user"));

        Crop crop = new Crop();
        crop.setCropName(cropDto.getCropName());
        crop.setSeason(cropDto.getSeason());
        crop.setSowingDate(cropDto.getSowingDate());
        crop.setExpectedHarvestDate(cropDto.getExpectedHarvestDate());
        crop.setVariety(cropDto.getVariety());
        crop.setPlantingArea(cropDto.getPlantingArea());
        crop.setExpectedYield(cropDto.getExpectedYield());
        crop.setNotes(cropDto.getNotes());
        crop.setStatus(cropDto.getStatus() != null ? cropDto.getStatus() : "PLANTED");
        crop.setFarm(farm);

        crop = cropRepository.save(crop);
        
        // Log activity
        userActivityService.logActivity(farm.getUser(), com.farmeazy.entity.UserActivity.ActivityType.CROP_PLANTED, "Planted crop '" + crop.getCropName() + "' in farm '" + farm.getFarmName() + "'");

        // Send in-app notification for crop creation
        try {
            notificationService.createForUser(
                farm.getUser(),
                com.farmeazy.entity.Notification.NotificationType.FARM,
                "Crop Added: " + crop.getCropName(),
                "New crop '" + crop.getCropName() + "' planted in '" + farm.getFarmName() + "'. Expected harvest: " + crop.getExpectedHarvestDate(),
                "/farms/" + farm.getId(),
                com.farmeazy.entity.Notification.NotificationPriority.NORMAL
            );
        } catch (Exception e) {
            logger.warn("CROP_SERVICE_CREATE_NOTIFICATION_FAILED userId={} cropId={} message={}", userId, crop.getId(), e.getMessage());
        }
        
        // Send notification email using HTTP service (works on Render)
        try {
            String message = "New crop '" + crop.getCropName() + "' has been successfully added to your farm '" + farm.getFarmName() + "'. "
                    + "Expected harvest date: " + crop.getExpectedHarvestDate();
            httpEmailService.sendNotification(farm.getUser().getEmail(), farm.getUser().getUsername(),
                "New Crop Added - FarmEazy", message);
        } catch (Exception e) {
            logger.warn("CROP_SERVICE_CREATE_EMAIL_FAILED userId={} cropId={} message={}", userId, crop.getId(), e.getMessage());
        }

        logger.info("CROP_SERVICE_CREATE_SUCCESS userId={} cropId={}", userId, crop.getId());
        
        return mapCropToDto(crop, farm.getUser().getId());
    }

    @Cacheable(cacheNames = "cropById", key = "#cropId + ':' + #userId", unless = "#result == null")
    public CropDto getCropById(Long cropId, Long userId) {
        logger.info("CROP_SERVICE_GET_BY_ID cropId={} userId={}", cropId, userId);
        Crop crop = cropRepository.findByIdAndFarmUserId(cropId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));

        return mapCropToDto(crop, userId);
    }

    @Cacheable(cacheNames = "cropListByUser", key = "#userId")
    public List<CropDto> getAllCropsByUser(Long userId) {
        logger.info("CROP_SERVICE_GET_ALL_BY_USER userId={} - DATABASE_HIT (cache miss, querying database)", userId);
        return cropRepository.findByFarmUserId(userId)
                .stream()
                .map(crop -> mapCropToDto(crop, userId))
                .toList();
    }

    @Cacheable(cacheNames = "cropListByFarm", key = "#farmId + ':' + #userId")
    public List<CropDto> getCropsByFarm(Long farmId, Long userId) {
        logger.info("CROP_SERVICE_GET_BY_FARM farmId={} userId={}", farmId, userId);
        Farm farm = farmRepository.findByIdAndUserId(farmId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found or not owned by user"));

        return cropRepository.findByFarmId(farmId)
                .stream()
                .map(crop -> mapCropToDto(crop, userId))
                .toList();
    }

    public Page<CropDto> getCropsByFarmPaginated(Long farmId, Long userId, Pageable pageable) {
        logger.info("CROP_SERVICE_GET_BY_FARM_PAGINATED farmId={} userId={} page={} size={}", farmId, userId, pageable.getPageNumber(), pageable.getPageSize());
        Farm farm = farmRepository.findByIdAndUserId(farmId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found or not owned by user"));

        return cropRepository.findByFarmId(farmId, pageable)
                .map(crop -> mapCropToDto(crop, userId));
    }

    public Page<CropDto> searchCrops(Long farmId, String cropName, Long userId, Pageable pageable) {
        logger.info("CROP_SERVICE_SEARCH farmId={} userId={} cropName={} page={} size={}", farmId, userId, cropName, pageable.getPageNumber(), pageable.getPageSize());
        Farm farm = farmRepository.findByIdAndUserId(farmId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found or not owned by user"));

        return cropRepository.findByFarmIdAndCropNameContainingIgnoreCase(farmId, cropName, pageable)
                .map(crop -> mapCropToDto(crop, userId));
    }

    @Transactional
        @Caching(evict = {
            @CacheEvict(cacheNames = "cropById", allEntries = true),
            @CacheEvict(cacheNames = "cropListByUser", key = "#userId"),
            @CacheEvict(cacheNames = "cropListByFarm", allEntries = true)
        })
    public CropDto updateCrop(Long cropId, CropDto cropDto, Long userId) {
        logger.info("CROP_SERVICE_UPDATE_START cropId={} userId={} cropName={}", cropId, userId, cropDto != null ? cropDto.getCropName() : null);
        Crop crop = cropRepository.findByIdAndFarmUserId(cropId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));

        crop.setCropName(cropDto.getCropName());
        crop.setSeason(cropDto.getSeason());
        crop.setSowingDate(cropDto.getSowingDate());
        crop.setExpectedHarvestDate(cropDto.getExpectedHarvestDate());
        crop.setVariety(cropDto.getVariety());
        crop.setPlantingArea(cropDto.getPlantingArea());
        crop.setExpectedYield(cropDto.getExpectedYield());
        crop.setNotes(cropDto.getNotes());
        crop.setStatus(cropDto.getStatus());

        crop = cropRepository.save(crop);
        
        // Log activity
        userActivityService.logActivity(crop.getFarm().getUser(), com.farmeazy.entity.UserActivity.ActivityType.CROP_UPDATED, "Updated crop '" + crop.getCropName() + "' in farm '" + crop.getFarm().getFarmName() + "'");

        // Send update email notification
        try {
            String message = "Crop '" + crop.getCropName() + "' in your farm '" + crop.getFarm().getFarmName() + "' has been updated. "
                    + "Expected harvest date: " + crop.getExpectedHarvestDate();
            httpEmailService.sendNotification(crop.getFarm().getUser().getEmail(), crop.getFarm().getUser().getUsername(),
                "Crop Updated - FarmEazy", message);
        } catch (Exception e) {
            logger.warn("CROP_SERVICE_UPDATE_EMAIL_FAILED cropId={} userId={} message={}", cropId, userId, e.getMessage());
        }

        logger.info("CROP_SERVICE_UPDATE_SUCCESS cropId={} userId={}", cropId, userId);
        
        return mapCropToDto(crop, userId);
    }

    @Transactional
        @Caching(evict = {
            @CacheEvict(cacheNames = "cropById", allEntries = true),
            @CacheEvict(cacheNames = "cropListByUser", key = "#userId"),
            @CacheEvict(cacheNames = "cropListByFarm", allEntries = true)
        })
    public void deleteCrop(Long cropId, Long userId) {
        logger.info("CROP_SERVICE_DELETE_START cropId={} userId={}", cropId, userId);
        Crop crop = cropRepository.findByIdAndFarmUserId(cropId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));

        // Log activity before deleting
        userActivityService.logActivity(crop.getFarm().getUser(), com.farmeazy.entity.UserActivity.ActivityType.CROP_DELETED, "Deleted crop '" + crop.getCropName() + "' from farm '" + crop.getFarm().getFarmName() + "'");

        // Send delete email notification
        try {
            String message = "Crop '" + crop.getCropName() + "' has been deleted from your farm '" + crop.getFarm().getFarmName() + ".";
            httpEmailService.sendNotification(crop.getFarm().getUser().getEmail(), crop.getFarm().getUser().getUsername(),
                "Crop Deleted - FarmEazy", message);
        } catch (Exception e) {
            logger.warn("CROP_SERVICE_DELETE_EMAIL_FAILED cropId={} userId={} message={}", cropId, userId, e.getMessage());
        }

        cropRepository.delete(crop);
        logger.info("CROP_SERVICE_DELETE_SUCCESS cropId={} userId={}", cropId, userId);
    }

    private CropDto mapCropToDto(Crop crop, Long userId) {
        CropDto dto = new CropDto();
        dto.setId(crop.getId());
        dto.setCropName(crop.getCropName());
        dto.setSeason(crop.getSeason());
        dto.setSowingDate(crop.getSowingDate());
        dto.setExpectedHarvestDate(crop.getExpectedHarvestDate());
        dto.setVariety(crop.getVariety());
        dto.setPlantingArea(crop.getPlantingArea());
        dto.setExpectedYield(crop.getExpectedYield());
        dto.setNotes(crop.getNotes());
        dto.setStatus(crop.getStatus());
        dto.setFarmId(crop.getFarm().getId());
        dto.setFarmUserId(userId);
        return dto;
    }
}

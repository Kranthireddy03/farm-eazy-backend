package com.farmeazy.service;

import com.farmeazy.dto.CropDto;
import com.farmeazy.entity.Crop;
import com.farmeazy.entity.Farm;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.CropRepository;
import com.farmeazy.repository.FarmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CropService {

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private HttpEmailService httpEmailService;

    @Transactional
    public CropDto createCrop(CropDto cropDto, Long userId) {
        Farm farm = farmRepository.findById(cropDto.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to add crops to this farm");
        }

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
        
        // Send notification email using HTTP service (works on Render)
        try {
            String message = "New crop '" + crop.getCropName() + "' has been successfully added to your farm '" + farm.getFarmName() + "'. "
                    + "Expected harvest date: " + crop.getExpectedHarvestDate();
            httpEmailService.sendNotification(farm.getUser().getEmail(), farm.getUser().getFullName(),
                "New Crop Added - FarmEazy", message);
        } catch (Exception e) {
            System.err.println("Failed to send crop creation email: " + e.getMessage());
        }
        
        return mapCropToDto(crop, farm.getUser().getId());
    }

    public CropDto getCropById(Long cropId, Long userId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));
        
        if (!crop.getFarm().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this crop");
        }

        return mapCropToDto(crop, userId);
    }

    public List<CropDto> getAllCropsByUser(Long userId) {
        return cropRepository.findAll()
                .stream()
                .filter(crop -> crop.getFarm().getUser().getId().equals(userId))
                .map(crop -> mapCropToDto(crop, userId))
                .toList();
    }

    public List<CropDto> getCropsByFarm(Long farmId, Long userId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this farm");
        }

        return cropRepository.findByFarmId(farmId)
                .stream()
                .map(crop -> mapCropToDto(crop, userId))
                .toList();
    }

    public Page<CropDto> getCropsByFarmPaginated(Long farmId, Long userId, Pageable pageable) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this farm");
        }

        return cropRepository.findByFarmId(farmId, pageable)
                .map(crop -> mapCropToDto(crop, userId));
    }

    public Page<CropDto> searchCrops(Long farmId, String cropName, Long userId, Pageable pageable) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this farm");
        }

        return cropRepository.findByFarmIdAndCropNameContainingIgnoreCase(farmId, cropName, pageable)
                .map(crop -> mapCropToDto(crop, userId));
    }

    @Transactional
    public CropDto updateCrop(Long cropId, CropDto cropDto, Long userId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));
        
        if (!crop.getFarm().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to update this crop");
        }

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
            httpEmailService.sendNotification(crop.getFarm().getUser().getEmail(), crop.getFarm().getUser().getFullName(),
                "Crop Updated - FarmEazy", message);
        } catch (Exception e) {
            System.err.println("Failed to send crop update email: " + e.getMessage());
        }
        
        return mapCropToDto(crop, userId);
    }

    @Transactional
    public void deleteCrop(Long cropId, Long userId) {
        Crop crop = cropRepository.findById(cropId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));
        
        if (!crop.getFarm().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to delete this crop");
        }

        // Log activity before deleting
        userActivityService.logActivity(crop.getFarm().getUser(), com.farmeazy.entity.UserActivity.ActivityType.CROP_DELETED, "Deleted crop '" + crop.getCropName() + "' from farm '" + crop.getFarm().getFarmName() + "'");

        // Send delete email notification
        try {
            String message = "Crop '" + crop.getCropName() + "' has been deleted from your farm '" + crop.getFarm().getFarmName() + ".";
            httpEmailService.sendNotification(crop.getFarm().getUser().getEmail(), crop.getFarm().getUser().getFullName(),
                "Crop Deleted - FarmEazy", message);
        } catch (Exception e) {
            System.err.println("Failed to send crop deletion email: " + e.getMessage());
        }

        cropRepository.delete(crop);
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

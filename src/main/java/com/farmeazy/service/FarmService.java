package com.farmeazy.service;

import com.farmeazy.dto.FarmDto;
import com.farmeazy.entity.Farm;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.FarmRepository;
import com.farmeazy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FarmService {

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private HttpEmailService httpEmailService;

    @Autowired
    private CoinService coinService;

    @Transactional
    public FarmDto createFarm(FarmDto farmDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Farm farm = new Farm();
        farm.setFarmName(farmDto.getFarmName());
        farm.setLocation(farmDto.getLocation());
        farm.setAreaSize(farmDto.getAreaSize());
        farm.setSoilType(farmDto.getSoilType());
        farm.setWaterSource(farmDto.getWaterSource());
        farm.setDescription(farmDto.getDescription());
        farm.setUser(user);

        farm = farmRepository.save(farm);

        // Log activity
        userActivityService.logActivity(user, com.farmeazy.entity.UserActivity.ActivityType.FARM_CREATED, "Created farm '" + farm.getFarmName() + "'");

        // Award coins for creating a farm (10 coins)
        try {
            coinService.addCoins(user.getEmail(), 10, "Created farm: " + farm.getFarmName());
        } catch (Exception e) {
            System.err.println("Failed to award coins for farm creation: " + e.getMessage());
        }

        // Send notification email using HTTP service (works on Render)
        try {
            String message = "New farm '" + farm.getFarmName() + "' has been successfully created. "
                    + "Location: " + farm.getLocation() + ", Area: " + farm.getAreaSize() + " acres.";
            httpEmailService.sendNotification(user.getEmail(), user.getUsername(),
                "New Farm Created - FarmEazy", message);
        } catch (Exception e) {
            System.err.println("Failed to send farm creation email: " + e.getMessage());
        }
        
        return mapFarmToDto(farm);
    }

    public FarmDto getFarmById(Long farmId, Long userId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to access this farm");
        }

        return mapFarmToDto(farm);
    }

    public List<FarmDto> getAllFarmsByUser(Long userId) {
        return farmRepository.findByUserId(userId)
                .stream()
                .map(this::mapFarmToDto)
                .toList();
    }

    public Page<FarmDto> getFarmsByUserPaginated(Long userId, Pageable pageable) {
        return farmRepository.findByUserId(userId, pageable)
                .map(this::mapFarmToDto);
    }

    public Page<FarmDto> searchFarms(Long userId, String farmName, Pageable pageable) {
        return farmRepository.findByUserIdAndFarmNameContainingIgnoreCase(userId, farmName, pageable)
                .map(this::mapFarmToDto);
    }

    @Transactional
    public FarmDto updateFarm(Long farmId, FarmDto farmDto, Long userId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to update this farm");
        }

        farm.setFarmName(farmDto.getFarmName());
        farm.setLocation(farmDto.getLocation());
        farm.setAreaSize(farmDto.getAreaSize());
        farm.setSoilType(farmDto.getSoilType());
        farm.setWaterSource(farmDto.getWaterSource());
        farm.setDescription(farmDto.getDescription());

        farm = farmRepository.save(farm);
        
        // Log activity
        userActivityService.logActivity(farm.getUser(), com.farmeazy.entity.UserActivity.ActivityType.FARM_UPDATED, "Updated farm '" + farm.getFarmName() + "'");

        // Send notification email after update
        try {
            String message = "Your farm '" + farm.getFarmName() + "' has been updated. "
                    + "Location: " + farm.getLocation() + ", Area: " + farm.getAreaSize() + " acres.";
            httpEmailService.sendNotification(farm.getUser().getEmail(), farm.getUser().getUsername(),
                "Farm Updated - FarmEazy", message);
        } catch (Exception e) {
            System.err.println("Failed to send farm update email: " + e.getMessage());
        }

        return mapFarmToDto(farm);
    }

    @Transactional
    public void deleteFarm(Long farmId, Long userId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        
        if (!farm.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not have permission to delete this farm");
        }

        // Log activity before deleting
        userActivityService.logActivity(farm.getUser(), com.farmeazy.entity.UserActivity.ActivityType.FARM_DELETED, "Deleted farm '" + farm.getFarmName() + "'");

        farmRepository.delete(farm);

        // Send notification email after deletion
        try {
            String message = "Your farm '" + farm.getFarmName() + "' has been deleted from your FarmEazy account. "
                    + "Location: " + farm.getLocation() + ", Area: " + farm.getAreaSize() + " acres.";
            httpEmailService.sendNotification(farm.getUser().getEmail(), farm.getUser().getUsername(),
                "Farm Deleted - FarmEazy", message);
        } catch (Exception e) {
            System.err.println("Failed to send farm deletion email: " + e.getMessage());
        }
    }

    private FarmDto mapFarmToDto(Farm farm) {
        FarmDto dto = new FarmDto();
        dto.setId(farm.getId());
        dto.setFarmName(farm.getFarmName());
        dto.setLocation(farm.getLocation());
        dto.setAreaSize(farm.getAreaSize());
        dto.setSoilType(farm.getSoilType());
        dto.setWaterSource(farm.getWaterSource());
        dto.setDescription(farm.getDescription());
        dto.setUserId(farm.getUser().getId());
        return dto;
    }
}

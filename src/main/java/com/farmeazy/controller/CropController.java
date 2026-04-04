package com.farmeazy.controller;

import com.farmeazy.dto.CropDto;
import com.farmeazy.service.CropService;
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

@RestController
@RequestMapping("/api/crops")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:4200",
    "http://localhost:3000",
    "http://localhost:3001"
})
@Tag(name = "Crop Management", description = "Crop CRUD operations")
public class CropController {

    private static final Logger logger = LoggerFactory.getLogger(CropController.class);

    @Autowired
    private CropService cropService;

    @Autowired
    private UserRepository userRepository;

    private Long getUserId(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        User user = userRepository.findByEmail(email).orElseThrow();
        return user.getId();
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a new crop")
    public ResponseEntity<CropDto> createCrop(@Valid @RequestBody CropDto cropDto,
                                              Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("CROP_CONTROLLER_CREATE userId={} farmId={} cropName={}", userId, cropDto != null ? cropDto.getFarmId() : null, cropDto != null ? cropDto.getCropName() : null);
        CropDto response = cropService.createCrop(cropDto, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{cropId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get crop by ID")
    public ResponseEntity<CropDto> getCropById(@PathVariable Long cropId,
                                               Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("CROP_CONTROLLER_GET_BY_ID userId={} cropId={}", userId, cropId);
        CropDto crop = cropService.getCropById(cropId, userId);
        return ResponseEntity.ok(crop);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get all crops for user")
    public ResponseEntity<List<CropDto>> getAllCrops(Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("CROP_CONTROLLER_GET_ALL userId={}", userId);
        List<CropDto> crops = cropService.getAllCropsByUser(userId);
        return ResponseEntity.ok(crops);
    }

    @GetMapping("/farm/{farmId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get all crops for a farm")
    public ResponseEntity<List<CropDto>> getCropsByFarm(@PathVariable Long farmId,
                                                        Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("CROP_CONTROLLER_GET_BY_FARM userId={} farmId={}", userId, farmId);
        List<CropDto> crops = cropService.getCropsByFarm(farmId, userId);
        return ResponseEntity.ok(crops);
    }

    @GetMapping("/farm/{farmId}/paginated")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get crops with pagination for a farm")
    public ResponseEntity<Page<CropDto>> getCropsByFarmPaginated(
            @PathVariable Long farmId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("CROP_CONTROLLER_GET_BY_FARM_PAGINATED userId={} farmId={} page={} size={}", userId, farmId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<CropDto> crops = cropService.getCropsByFarmPaginated(farmId, userId, pageable);
        return ResponseEntity.ok(crops);
    }

    @GetMapping("/farm/{farmId}/search")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Search crops by name in a farm")
    public ResponseEntity<Page<CropDto>> searchCrops(
            @PathVariable Long farmId,
            @RequestParam String cropName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("CROP_CONTROLLER_SEARCH userId={} farmId={} cropName={} page={} size={}", userId, farmId, cropName, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<CropDto> crops = cropService.searchCrops(farmId, cropName, userId, pageable);
        return ResponseEntity.ok(crops);
    }

    @PutMapping("/{cropId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update a crop")
    public ResponseEntity<CropDto> updateCrop(@PathVariable Long cropId,
                                              @Valid @RequestBody CropDto cropDto,
                                              Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("CROP_CONTROLLER_UPDATE userId={} cropId={}", userId, cropId);
        CropDto response = cropService.updateCrop(cropId, cropDto, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{cropId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Delete a crop")
    public ResponseEntity<Void> deleteCrop(@PathVariable Long cropId,
                                           Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("CROP_CONTROLLER_DELETE userId={} cropId={}", userId, cropId);
        cropService.deleteCrop(cropId, userId);
        return ResponseEntity.noContent().build();
    }
}

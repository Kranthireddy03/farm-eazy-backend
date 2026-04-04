package com.farmeazy.controller;

import com.farmeazy.dto.FarmDto;
import com.farmeazy.service.FarmService;
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
@RequestMapping("/api/farms")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:4200",
    "http://localhost:3000",
    "http://localhost:3001"
})
@Tag(name = "Farm Management", description = "Farm CRUD operations")
public class FarmController {

    private static final Logger logger = LoggerFactory.getLogger(FarmController.class);

    @Autowired
    private FarmService farmService;

    @Autowired
    private UserRepository userRepository;

    private Long getUserId(Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        User user = userRepository.findByEmail(email).orElseThrow();
        return user.getId();
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a new farm")
    public ResponseEntity<FarmDto> createFarm(@Valid @RequestBody FarmDto farmDto, 
                                              Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("FARM_CONTROLLER_CREATE userId={} farmName={}", userId, farmDto != null ? farmDto.getFarmName() : null);
        FarmDto response = farmService.createFarm(farmDto, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{farmId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get farm by ID")
    public ResponseEntity<FarmDto> getFarmById(@PathVariable Long farmId, 
                                               Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("FARM_CONTROLLER_GET_BY_ID userId={} farmId={}", userId, farmId);
        FarmDto farm = farmService.getFarmById(farmId, userId);
        return ResponseEntity.ok(farm);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get all farms")
    public ResponseEntity<List<FarmDto>> getAllFarms(Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("FARM_CONTROLLER_GET_ALL userId={}", userId);
        List<FarmDto> farms = farmService.getAllFarmsByUser(userId);
        return ResponseEntity.ok(farms);
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get farms with pagination")
    public ResponseEntity<Page<FarmDto>> getFarmsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("FARM_CONTROLLER_GET_PAGINATED userId={} page={} size={}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<FarmDto> farms = farmService.getFarmsByUserPaginated(userId, pageable);
        return ResponseEntity.ok(farms);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Search farms by name")
    public ResponseEntity<Page<FarmDto>> searchFarms(
            @RequestParam String farmName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("FARM_CONTROLLER_SEARCH userId={} farmName={} page={} size={}", userId, farmName, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<FarmDto> farms = farmService.searchFarms(userId, farmName, pageable);
        return ResponseEntity.ok(farms);
    }

    @PutMapping("/{farmId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update a farm")
    public ResponseEntity<FarmDto> updateFarm(@PathVariable Long farmId, 
                                              @Valid @RequestBody FarmDto farmDto,
                                              Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("FARM_CONTROLLER_UPDATE userId={} farmId={}", userId, farmId);
        FarmDto response = farmService.updateFarm(farmId, farmDto, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{farmId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Delete a farm")
    public ResponseEntity<Void> deleteFarm(@PathVariable Long farmId, 
                                           Authentication authentication) {
        Long userId = getUserId(authentication);
        logger.info("FARM_CONTROLLER_DELETE userId={} farmId={}", userId, farmId);
        farmService.deleteFarm(farmId, userId);
        return ResponseEntity.noContent().build();
    }
}

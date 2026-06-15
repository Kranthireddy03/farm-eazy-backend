package com.farmeazy.controller;

import com.farmeazy.dto.ServiceBookingDto;
import com.farmeazy.dto.ServiceListingDto;
import com.farmeazy.dto.ServiceBookingCreateDto;
import com.farmeazy.dto.ServiceBookingResponseDto;
import com.farmeazy.dto.ServiceListingCreateDto;
import com.farmeazy.dto.ServiceListingResponseDto;
import com.farmeazy.entity.UserBankDetails;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.UserBankDetailsRepository;
import com.farmeazy.service.FileStorageService;
import com.farmeazy.service.ListingEligibilityService;
import com.farmeazy.service.ServiceService;
import com.farmeazy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:4200",
    "http://localhost:3000"
})
@Tag(name = "Services", description = "Service listings and bookings management")
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private UserService userService;

    @Autowired
    private ListingEligibilityService listingEligibilityService;

    @Autowired
    private UserBankDetailsRepository userBankDetailsRepository;

    @Autowired
    private FileStorageService fileStorageService;


    @PostMapping("/listings")
    public ResponseEntity<ServiceListingDto> createServiceListing(@Valid @RequestBody ServiceListingDto serviceListingDto, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        listingEligibilityService.assertEligible(user, "SERVICE");

        UserBankDetails bankDetails = userBankDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor details not found. Please complete bank details first."));

        // Always derive vendor profile server-side to prevent payload-based onboarding.
        serviceListingDto.setVendorId(user.getId());
        serviceListingDto.setVendorName(bankDetails.getAccountHolderName());
        serviceListingDto.setVendorLocation(formatLocation(user));
        serviceListingDto.setVendorType("VERIFIED_VENDOR");

        ServiceListingDto createdListing = serviceService.createServiceListing(user, serviceListingDto);
        return new ResponseEntity<>(createdListing, HttpStatus.CREATED);
    }

    @PutMapping("/listings/{id}")
    public ResponseEntity<ServiceListingDto> updateServiceListing(@PathVariable Long id, @Valid @RequestBody ServiceListingDto serviceListingDto, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        ServiceListingDto updated = serviceService.updateServiceListing(id, user, serviceListingDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<Void> deleteServiceListing(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        serviceService.deleteServiceListing(id, user);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/listings")
    public ResponseEntity<Page<ServiceListingDto>> getServiceListings(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Principal principal) {
        User user = null;
        if (principal != null) {
            user = userService.findByEmail(principal.getName());
        }
        Page<ServiceListingDto> listings = serviceService.getServiceListings(PageRequest.of(page, size), user);
        return ResponseEntity.ok(listings);
    }

    @GetMapping("/listings/my")
    public ResponseEntity<Page<ServiceListingDto>> getMyServiceListings(Principal principal, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        User user = userService.findByEmail(principal.getName());
        Page<ServiceListingDto> listings = serviceService.getUserServiceListings(user, PageRequest.of(page, size));
        return ResponseEntity.ok(listings);
    }

    @PostMapping("/bookings")
    public ResponseEntity<ServiceBookingDto> createServiceBooking(@Valid @RequestBody ServiceBookingDto serviceBookingDto, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        ServiceBookingDto createdBooking = serviceService.createServiceBooking(user, serviceBookingDto);
        return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
    }

    @GetMapping("/bookings/my-bookings")
    public ResponseEntity<Page<ServiceBookingDto>> getUserBookings(Principal principal, Pageable pageable) {
        User user = userService.findByEmail(principal.getName());
        Page<ServiceBookingDto> bookings = serviceService.getUserBookings(user, pageable);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/my-listings")
    public ResponseEntity<Page<ServiceBookingDto>> getProviderBookings(Principal principal, Pageable pageable) {
        User user = userService.findByEmail(principal.getName());
        Page<ServiceBookingDto> bookings = serviceService.getProviderBookings(user, pageable);
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/bookings/{id}/approve")
    public ResponseEntity<ServiceBookingDto> approveBooking(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        ServiceBookingDto approved = serviceService.approveBooking(id, user);
        return ResponseEntity.ok(approved);
    }

    @PutMapping("/bookings/{id}/decline")
    public ResponseEntity<ServiceBookingDto> declineBooking(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        ServiceBookingDto declined = serviceService.declineBooking(id, user);
        return ResponseEntity.ok(declined);
    }

    // ======================== ENHANCED MARKETPLACE ENDPOINTS ========================

    /**
     * Create enhanced service listing with pricing breakdown.
     */
    @PostMapping("/listings/enhanced")
    @Operation(summary = "Create enhanced listing", description = "Create service listing with detailed pricing (machine, driver, etc.)")
    public ResponseEntity<ServiceListingResponseDto> createEnhancedServiceListing(
            @Valid @RequestBody ServiceListingCreateDto dto, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        listingEligibilityService.assertEligible(user, "SERVICE");

        UserBankDetails bankDetails = userBankDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor details not found. Please complete bank details first."));
        dto.setVendorId(user.getId());
        dto.setVendorName(bankDetails.getAccountHolderName());
        dto.setVendorLocation(formatLocation(user));
        dto.setVendorType("VERIFIED_VENDOR");

        ServiceListingResponseDto created = serviceService.createEnhancedServiceListing(user, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    private String formatLocation(User user) {
        String city = user.getCity() != null ? user.getCity().trim() : "";
        String state = user.getState() != null ? user.getState().trim() : "";
        if (!city.isEmpty() && !state.isEmpty()) {
            return city + ", " + state;
        }
        return !city.isEmpty() ? city : state;
    }

    @PostMapping(value = "/attachments/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(summary = "Upload service attachments", description = "Upload image/video files for service listings")
    public ResponseEntity<Map<String, Object>> uploadAttachments(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No files were provided"));
        }

        for (MultipartFile file : files) {
            String fileName = fileStorageService.store(file);
            String fileDownloadUri = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/services/media/")
                    .path(fileName)
                    .toUriString();
            urls.add(fileDownloadUri);
        }

        return ResponseEntity.ok(Map.of("urls", urls, "count", urls.size()));
    }

    @GetMapping("/media/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveServiceMedia(@PathVariable String filename) {
        Resource file = fileStorageService.loadAsResource(filename);
        String contentType = detectContentType(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(file);
    }

    private String detectContentType(String filename) {
        String safeName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (safeName.endsWith(".png")) return "image/png";
        if (safeName.endsWith(".jpg") || safeName.endsWith(".jpeg")) return "image/jpeg";
        if (safeName.endsWith(".webp")) return "image/webp";
        if (safeName.endsWith(".gif")) return "image/gif";
        if (safeName.endsWith(".mp4")) return "video/mp4";
        if (safeName.endsWith(".webm")) return "video/webm";
        if (safeName.endsWith(".ogg")) return "video/ogg";
        if (safeName.endsWith(".m4v")) return "video/x-m4v";
        if (safeName.endsWith(".mov")) return "video/quicktime";
        if (safeName.endsWith(".pdf")) return "application/pdf";
        if (safeName.endsWith(".txt")) return "text/plain";
        if (safeName.endsWith(".csv")) return "text/csv";
        if (safeName.endsWith(".doc")) return "application/msword";
        if (safeName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (safeName.endsWith(".xls")) return "application/vnd.ms-excel";
        if (safeName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }

    /**
     * Get enhanced service listing details.
     */
    @GetMapping("/listings/{id}/details")
    @Operation(summary = "Get listing details", description = "Get detailed service listing with all pricing info")
    public ResponseEntity<ServiceListingResponseDto> getEnhancedServiceListing(@PathVariable Long id) {
        ServiceListingResponseDto listing = serviceService.getEnhancedServiceListing(id);
        return ResponseEntity.ok(listing);
    }

    /**
     * Get price breakdown for a service listing.
     */
    @GetMapping("/listings/{id}/price-breakdown")
    @Operation(summary = "Get price breakdown", description = "Calculate price breakdown for hours, driver option, etc.")
    public ResponseEntity<Map<String, Object>> getPriceBreakdown(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer hours,
            @RequestParam(defaultValue = "false") Boolean includeDriver,
            @RequestParam(required = false) Integer workers) {
        Map<String, Object> breakdown = serviceService.getPriceBreakdown(id, hours, includeDriver, workers);
        return ResponseEntity.ok(breakdown);
    }

    /**
     * Create enhanced service booking with payment calculation.
     */
    @PostMapping("/bookings/enhanced")
    @Operation(summary = "Create enhanced booking", description = "Create booking with automatic price calculation")
    public ResponseEntity<ServiceBookingResponseDto> createEnhancedServiceBooking(
            @Valid @RequestBody ServiceBookingCreateDto dto, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        ServiceBookingResponseDto created = serviceService.createEnhancedServiceBooking(user, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * Process payment for a booking (after Razorpay payment success).
     */
    @PostMapping("/bookings/{id}/payment")
    @Operation(summary = "Process booking payment", description = "Record successful payment for a booking")
    public ResponseEntity<ServiceBookingResponseDto> processBookingPayment(
            @PathVariable Long id,
            @RequestParam String razorpayOrderId,
            @RequestParam String razorpayPaymentId,
            @RequestParam(defaultValue = "RAZORPAY") String paymentMethod) {
        ServiceBookingResponseDto updated = serviceService.processBookingPayment(id, razorpayOrderId, razorpayPaymentId, paymentMethod);
        return ResponseEntity.ok(updated);
    }

    /**
     * Mark booking as complete (triggers payout process).
     */
    @PutMapping("/bookings/{id}/complete")
    @Operation(summary = "Complete booking", description = "Mark booking as completed (triggers payout to provider)")
    public ResponseEntity<ServiceBookingResponseDto> completeBooking(@PathVariable Long id, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        ServiceBookingResponseDto completed = serviceService.completeBooking(id, user);
        return ResponseEntity.ok(completed);
    }
}

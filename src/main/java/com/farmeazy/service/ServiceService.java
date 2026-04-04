package com.farmeazy.service;


import com.farmeazy.dto.ServiceBookingDto;
import com.farmeazy.dto.ServiceListingDto;
import com.farmeazy.dto.ServiceBookingCreateDto;
import com.farmeazy.dto.ServiceBookingResponseDto;
import com.farmeazy.dto.ServiceListingCreateDto;
import com.farmeazy.dto.ServiceListingResponseDto;
import com.farmeazy.entity.Crop;
import com.farmeazy.entity.Farm;
import com.farmeazy.entity.Notification.NotificationPriority;
import com.farmeazy.entity.Notification.NotificationType;
import com.farmeazy.entity.ServiceBooking;
import com.farmeazy.entity.ServiceBooking.BookingStatus;
import com.farmeazy.entity.ServiceBooking.PaymentStatus;
import com.farmeazy.entity.ServiceBooking.PayoutStatus;
import com.farmeazy.entity.ServiceListing;
import com.farmeazy.entity.ServiceListing.PriceUnit;
import com.farmeazy.entity.ServiceAttribute;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.CropRepository;
import com.farmeazy.repository.FarmRepository;
import com.farmeazy.repository.ServiceAttributeRepository;
import com.farmeazy.repository.ServiceBookingRepository;
import com.farmeazy.repository.ServiceListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class ServiceService {

    @Autowired
    private HttpEmailService httpEmailService;

    @Autowired
    private ServiceListingRepository serviceListingRepository;

    @Autowired
    private ServiceBookingRepository serviceBookingRepository;

    @Autowired
    private ServiceAttributeRepository serviceAttributeRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private SmsService smsService;

    @Autowired
    private NotificationService notificationService;

    @Value("${farmeazy.platform.fee.percentage:5.00}")
    private BigDecimal platformFeePercentage;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional
    public ServiceListingDto createServiceListing(User user, ServiceListingDto dto) {
        ServiceListing entity = new ServiceListing();
        entity.setTitle(dto.getServiceName());
        entity.setUser(user);
        entity.setVendorId(dto.getVendorId());
        entity.setVendorName(dto.getVendorName());
        entity.setVendorLocation(dto.getVendorLocation());
        entity.setVendorType(dto.getVendorType());
        entity.setRate(dto.getPrice());
        entity.setContactName(dto.getContactName() != null ? dto.getContactName() : user.getUsername());
        entity.setContactPhone(dto.getContactPhone() != null ? dto.getContactPhone() : user.getPhone());
        entity.setContactEmail(dto.getContactEmail() != null ? dto.getContactEmail() : user.getEmail());
        entity.setDescription(dto.getDescription());
        if (dto.getAttachmentUrls() != null && !dto.getAttachmentUrls().isEmpty()) {
            String joined = dto.getAttachmentUrls().stream()
                    .filter(url -> url != null && !url.isBlank())
                    .map(String::trim)
                    .collect(Collectors.joining(","));
            entity.setAttachmentUrls(joined.isBlank() ? null : joined);
        } else {
            entity.setAttachmentUrls(null);
        }
        entity.setType(dto.getType());
        entity.setLocation(dto.getLocation());
        entity.setAvailability(dto.getAvailability() != null ? dto.getAvailability() : "Available");

        ServiceListing saved = serviceListingRepository.save(entity);

        try {
            notificationService.createForUser(
                user,
                NotificationType.PRODUCT,
                "Service Listed Successfully",
                "Your service has been listed: " + saved.getTitle(),
                "/services",
                NotificationPriority.NORMAL
            );
        } catch (Exception ignored) {
        }

        // Send service listing creation email notification
        try {
            httpEmailService.sendServiceListingCreatedNotification(
                user.getEmail(),
                user.getUsername(),
                saved.getTitle(),
                saved.getRate(),
                saved.getDescription()
            );
        } catch (Exception e) {
            System.err.println("Failed to send service listing creation email: " + e.getMessage());
        }
        return toDto(saved);
    }

    public Page<ServiceListingDto> getServiceListings(Pageable pageable) {
        // This method should be overloaded to accept the current user, so we can filter out their own listings
        throw new UnsupportedOperationException("Use getServiceListings(Pageable pageable, User user) instead.");
    }

    // New method to get listings for browse, excluding user's own
    public Page<ServiceListingDto> getServiceListings(Pageable pageable, User user) {
        if (user == null) {
            return serviceListingRepository.findAll(pageable).map(this::toDto);
        }
        return serviceListingRepository.findByUserIdNot(user.getId(), pageable).map(this::toDto);
    }

    public Page<ServiceListingDto> getUserServiceListings(User user, Pageable pageable) {
        return serviceListingRepository.findByUserId(user.getId(), pageable).map(this::toDto);
    }

    @Transactional
    public ServiceListingDto updateServiceListing(Long id, User user, ServiceListingDto dto) {
        ServiceListing entity = serviceListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service listing not found: " + id));
        if (!entity.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to update this service");
        }
        entity.setTitle(dto.getServiceName());
        entity.setRate(dto.getPrice());
        entity.setDescription(dto.getDescription());
        if (dto.getType() != null) {
            entity.setType(dto.getType());
        }
        if (dto.getLocation() != null) {
            entity.setLocation(dto.getLocation());
        }
        if (dto.getAvailability() != null) {
            entity.setAvailability(dto.getAvailability());
        }
        if (dto.getContactName() != null) {
            entity.setContactName(dto.getContactName());
        }
        if (dto.getContactPhone() != null) {
            entity.setContactPhone(dto.getContactPhone());
        }
        if (dto.getContactEmail() != null) {
            entity.setContactEmail(dto.getContactEmail());
        }
        if (dto.getAttachmentUrls() != null) {
            String joined = dto.getAttachmentUrls().stream()
                    .filter(url -> url != null && !url.isBlank())
                    .map(String::trim)
                    .collect(Collectors.joining(","));
            entity.setAttachmentUrls(joined.isBlank() ? null : joined);
        }

        ServiceListing updated = serviceListingRepository.save(entity);

        try {
            notificationService.createForUser(
                user,
                NotificationType.PRODUCT,
                "Service Updated",
                "Your service listing has been updated: " + updated.getTitle(),
                "/services",
                NotificationPriority.NORMAL
            );
        } catch (Exception ignored) {
        }

        // Send service listing update email notification
        try {
            httpEmailService.sendServiceListingUpdatedNotification(
                user.getEmail(),
                user.getUsername(),
                updated.getTitle(),
                updated.getRate(),
                updated.getDescription()
            );
        } catch (Exception e) {
            System.err.println("Failed to send service listing update email: " + e.getMessage());
        }
        return toDto(updated);
    }

    @Transactional
    public void deleteServiceListing(Long id, User user) {
        ServiceListing entity = serviceListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service listing not found: " + id));
        if (!entity.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this service");
        }
        serviceListingRepository.delete(entity);

        try {
            notificationService.createForUser(
                user,
                NotificationType.PRODUCT,
                "Service Removed",
                "Your service listing has been removed: " + entity.getTitle(),
                "/services",
                NotificationPriority.NORMAL
            );
        } catch (Exception ignored) {
        }

        // Send service listing deletion email notification
        try {
            httpEmailService.sendServiceListingDeletedNotification(
                user.getEmail(),
                user.getUsername(),
                entity.getTitle(),
                entity.getRate(),
                entity.getDescription()
            );
        } catch (Exception e) {
            System.err.println("Failed to send service listing deletion email: " + e.getMessage());
        }
    }

    private ServiceListingDto toDto(ServiceListing entity) {
        ServiceListingDto dto = new ServiceListingDto();
        dto.setId(entity.getId());
        dto.setServiceName(entity.getTitle());
        dto.setPrice(entity.getRate() != null ? entity.getRate() : 0.0);
        dto.setProviderId(entity.getUser() != null ? entity.getUser().getId() : null);
        dto.setVendorId(entity.getVendorId());
        dto.setVendorName(entity.getVendorName());
        dto.setVendorLocation(entity.getVendorLocation());
        dto.setVendorType(entity.getVendorType());
        dto.setDescription(entity.getDescription());
        dto.setType(entity.getType());
        dto.setLocation(entity.getLocation());
        dto.setAvailability(entity.getAvailability());
        dto.setContactName(entity.getContactName());
        dto.setContactPhone(entity.getContactPhone());
        dto.setContactEmail(entity.getContactEmail());
        if (entity.getAttachmentUrls() != null && !entity.getAttachmentUrls().isBlank()) {
            dto.setAttachmentUrls(Arrays.stream(entity.getAttachmentUrls().split(","))
                    .map(String::trim)
                    .filter(url -> !url.isBlank())
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    // Booking methods
    @Transactional
    public ServiceBookingDto createServiceBooking(User user, ServiceBookingDto dto) {
        ServiceBooking booking = new ServiceBooking();
        booking.setUser(user);
        booking.setServiceType(dto.getServiceType());
        booking.setLocation(dto.getLocation());
        booking.setHours(dto.getHours());
        booking.setPeopleCount(dto.getPeopleCount());
        booking.setNotes(dto.getNotes());
        booking.setStatus(BookingStatus.PENDING);

        // Set farm
        if (dto.getFarmId() != null) {
            Farm farm = farmRepository.findById(dto.getFarmId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
            booking.setFarm(farm);
        }

        // Set crop
        if (dto.getCropId() != null) {
            Crop crop = cropRepository.findById(dto.getCropId())
                    .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));
            booking.setCrop(crop);
        }

        // Set service listing if specified
        if (dto.getServiceListingId() != null) {
            ServiceListing listing = serviceListingRepository.findById(dto.getServiceListingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service listing not found"));
            // Prevent user from booking their own service
            if (listing.getUser() != null && listing.getUser().getId().equals(user.getId())) {
                throw new UnauthorizedException("You cannot book your own service listing.");
            }
            booking.setServiceListing(listing);
        }

        ServiceBooking saved = serviceBookingRepository.save(booking);

        try {
            notificationService.createForUser(
                user,
                NotificationType.PRODUCT,
                "Service booking requested",
                "Your booking request has been submitted.",
                "/irrigation-services",
                NotificationPriority.NORMAL
            );
            if (saved.getServiceListing() != null && saved.getServiceListing().getUser() != null) {
                notificationService.createForUser(
                    saved.getServiceListing().getUser(),
                    NotificationType.PRODUCT,
                    "New service booking request",
                    "A user requested your service: " + saved.getServiceListing().getTitle(),
                    "/irrigation-services",
                    NotificationPriority.HIGH
                );
            }
        } catch (Exception ignored) {
        }

        return toBookingDto(saved);
    }

    public Page<ServiceBookingDto> getUserBookings(User user, Pageable pageable) {
        return serviceBookingRepository.findByUserId(user.getId(), pageable)
                .map(this::toBookingDto);
    }

    public Page<ServiceBookingDto> getProviderBookings(User user, Pageable pageable) {
        return serviceBookingRepository.findByProviderId(user.getId(), pageable)
                .map(this::toBookingDto);
    }

    @Transactional
    public ServiceBookingDto approveBooking(Long bookingId, User provider) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        // Verify the user is the provider of the service listing
        if (booking.getServiceListing() == null ||
                !booking.getServiceListing().getUser().getId().equals(provider.getId())) {
            throw new UnauthorizedException("You are not authorized to approve this booking");
        }

        // Check if already processed
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is already " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.APPROVED);
        ServiceBooking updated = serviceBookingRepository.save(booking);

        try {
            notificationService.createForUser(
                booking.getUser(),
                NotificationType.PRODUCT,
                "Service booking approved",
                "Your booking #" + updated.getId() + " has been approved.",
                "/irrigation-services",
                NotificationPriority.HIGH
            );
        } catch (Exception ignored) {
        }

        // Send email notifications
        try {
            httpEmailService.sendServiceBookingApprovedNotification(
                    booking.getUser().getEmail(),
                    booking.getUser().getUsername(),
                    booking.getServiceType().toString(),
                    booking.getLocation(),
                    provider.getUsername()
            );
        } catch (Exception e) {
            System.err.println("Failed to send booking approval email: " + e.getMessage());
        }

        // Send Service Started SMS
        try {
            String userPhone = booking.getUser().getPhone();
            if (userPhone != null && !userPhone.isBlank()) {
                smsService.sendServiceStarted(
                    userPhone,
                    booking.getUser().getUsername(),
                    String.valueOf(booking.getId())
                );
            }
        } catch (Exception smsEx) {
            System.err.println("Failed to send service started SMS: " + smsEx.getMessage());
        }

        return toBookingDto(updated);
    }

    @Transactional
    public ServiceBookingDto declineBooking(Long bookingId, User provider) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        // Verify the user is the provider of the service listing
        if (booking.getServiceListing() == null ||
                !booking.getServiceListing().getUser().getId().equals(provider.getId())) {
            throw new UnauthorizedException("You are not authorized to decline this booking");
        }

        // Check if already processed
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is already " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.DECLINED);
        ServiceBooking updated = serviceBookingRepository.save(booking);

        try {
            notificationService.createForUser(
                booking.getUser(),
                NotificationType.PRODUCT,
                "Service booking declined",
                "Your booking #" + updated.getId() + " was declined.",
                "/irrigation-services",
                NotificationPriority.NORMAL
            );
        } catch (Exception ignored) {
        }

        // Send email notifications
        try {
            httpEmailService.sendServiceBookingDeclinedNotification(
                    booking.getUser().getEmail(),
                    booking.getUser().getUsername(),
                    booking.getServiceType().toString(),
                    booking.getLocation(),
                    provider.getUsername()
            );
        } catch (Exception e) {
            System.err.println("Failed to send booking decline email: " + e.getMessage());
        }

        return toBookingDto(updated);
    }

    // ======================== ENHANCED MARKETPLACE METHODS ========================

    /**
     * Create enhanced service listing with pricing breakdown.
     */
    @Transactional
    public ServiceListingResponseDto createEnhancedServiceListing(User user, ServiceListingCreateDto dto) {
        ServiceListing entity = new ServiceListing();
        entity.setUser(user);
        entity.setVendorId(dto.getVendorId());
        entity.setVendorName(dto.getVendorName());
        entity.setVendorLocation(dto.getVendorLocation());
        entity.setVendorType(dto.getVendorType());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setLocation(dto.getLocation());
        entity.setRate(dto.getRate());
        entity.setType(ServiceListing.ServiceType.valueOf(dto.getType()));
        
        // Enhanced pricing
        entity.setBasePrice(dto.getBasePrice());
        entity.setHasDriver(dto.getHasDriver());
        entity.setDriverPrice(dto.getDriverPrice());
        entity.setMachinePrice(dto.getMachinePrice());
        entity.setPriceUnit(dto.getPriceUnit() != null ? 
            PriceUnit.valueOf(dto.getPriceUnit()) : PriceUnit.PER_HOUR);
        
        // Equipment details
        entity.setFuelIncluded(dto.getFuelIncluded());
        entity.setOperatorIncluded(dto.getOperatorIncluded());
        entity.setMinimumHours(dto.getMinimumHours());
        entity.setMaximumHours(dto.getMaximumHours());
        entity.setServiceRadiusKm(dto.getServiceRadiusKm());
        entity.setEquipmentPower(dto.getEquipmentPower());
        entity.setEquipmentModel(dto.getEquipmentModel());
        
        // Convert implements list to JSON
        if (dto.getImplementsAvailable() != null && !dto.getImplementsAvailable().isEmpty()) {
            try {
                entity.setImplementsAvailable(objectMapper.writeValueAsString(dto.getImplementsAvailable()));
            } catch (JsonProcessingException e) {
                entity.setImplementsAvailable(String.join(",", dto.getImplementsAvailable()));
            }
        }
        
        // Manual labor specific
        entity.setWorkersCount(dto.getWorkersCount());
        entity.setToolsIncluded(dto.getToolsIncluded());
        entity.setExperienceYears(dto.getExperienceYears());
        
        // Contact details
        entity.setContactName(dto.getContactName() != null ? dto.getContactName() : user.getUsername());
        entity.setContactPhone(dto.getContactPhone() != null ? dto.getContactPhone() : user.getPhone());
        entity.setContactEmail(dto.getContactEmail() != null ? dto.getContactEmail() : user.getEmail());
        entity.setAvailability(dto.getAvailability());
        
        // Status
        entity.setIsActive(true);
        entity.setPayoutStatus(ServiceListing.PayoutStatus.NOT_APPLICABLE);

        ServiceListing saved = serviceListingRepository.save(entity);

        try {
            notificationService.createForUser(
                user,
                NotificationType.PRODUCT,
                "Service Listed Successfully",
                "Your service has been listed: " + saved.getTitle(),
                "/services",
                NotificationPriority.NORMAL
            );
        } catch (Exception ignored) {
        }

        // Save custom attributes
        if (dto.getCustomAttributes() != null && !dto.getCustomAttributes().isEmpty()) {
            saveServiceAttributes(saved, dto.getCustomAttributes());
        }

        // Send notification
        try {
            httpEmailService.sendServiceListingCreatedNotification(
                user.getEmail(),
                user.getUsername(),
                saved.getTitle(),
                saved.getRate(),
                saved.getDescription()
            );
        } catch (Exception e) {
            System.err.println("Failed to send service listing creation email: " + e.getMessage());
        }

        return toEnhancedDto(saved);
    }

    /**
     * Save custom service attributes.
     */
    private void saveServiceAttributes(ServiceListing listing, Map<String, Object> attributes) {
        int order = 0;
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            ServiceAttribute attr = new ServiceAttribute();
            attr.setServiceListing(listing);
            attr.setAttributeKey(entry.getKey());
            attr.setAttributeValue(String.valueOf(entry.getValue()));
            attr.setDisplayOrder(order++);
            
            // Determine attribute type
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                attr.setAttributeType(ServiceAttribute.AttributeType.BOOLEAN);
            } else if (value instanceof Number) {
                attr.setAttributeType(ServiceAttribute.AttributeType.NUMBER);
            } else {
                attr.setAttributeType(ServiceAttribute.AttributeType.STRING);
            }
            
            serviceAttributeRepository.save(attr);
        }
    }

    /**
     * Get enhanced service listing by ID.
     */
    public ServiceListingResponseDto getEnhancedServiceListing(Long id) {
        ServiceListing listing = serviceListingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service listing not found: " + id));
        return toEnhancedDto(listing);
    }

    /**
     * Create enhanced service booking with payment calculation.
     */
    @Transactional
    public ServiceBookingResponseDto createEnhancedServiceBooking(User user, ServiceBookingCreateDto dto) {
        ServiceListing listing = serviceListingRepository.findById(dto.getServiceListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Service listing not found"));

        // Prevent booking own service
        if (listing.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You cannot book your own service listing.");
        }

        Farm farm = farmRepository.findById(dto.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found"));

        Crop crop = null;
        if (dto.getCropId() != null) {
            crop = cropRepository.findById(dto.getCropId())
                    .orElseThrow(() -> new ResourceNotFoundException("Crop not found"));
        }

        ServiceBooking booking = new ServiceBooking();
        booking.setUser(user);
        booking.setProvider(listing.getUser());
        booking.setServiceListing(listing);
        booking.setServiceType(listing.getType());
        booking.setFarm(farm);
        booking.setCrop(crop);
        booking.setLocation(dto.getLocation());
        booking.setHours(dto.getHours());
        booking.setPeopleCount(dto.getPeopleCount());
        booking.setNotes(dto.getNotes());
        booking.setServiceDate(dto.getServiceDate());
        booking.setStartTime(dto.getStartTime());
        booking.setEndTime(dto.getEndTime());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setPayoutStatus(PayoutStatus.PENDING);

        // Calculate pricing
        calculateBookingPricing(booking, listing, dto);

        ServiceBooking saved = serviceBookingRepository.save(booking);

        try {
            notificationService.createForUser(
                user,
                NotificationType.PRODUCT,
                "Enhanced booking requested",
                "Your service booking request has been submitted.",
                "/irrigation-services",
                NotificationPriority.NORMAL
            );
            if (listing.getUser() != null) {
                notificationService.createForUser(
                    listing.getUser(),
                    NotificationType.PRODUCT,
                    "New booking for your service",
                    "A user requested your service: " + listing.getTitle(),
                    "/irrigation-services",
                    NotificationPriority.HIGH
                );
            }
        } catch (Exception ignored) {
        }

        return toEnhancedBookingDto(saved);
    }

    /**
     * Calculate booking pricing based on service listing rates.
     */
    private void calculateBookingPricing(ServiceBooking booking, ServiceListing listing, ServiceBookingCreateDto dto) {
        BigDecimal machineAmount = BigDecimal.ZERO;
        BigDecimal driverAmount = BigDecimal.ZERO;
        BigDecimal labourAmount = BigDecimal.ZERO;
        int hours = dto.getHours();

        // Calculate based on service type
        switch (listing.getType()) {
            case TRACTOR:
            case JCB:
            case HARVESTER:
                // Machine pricing
                if (listing.getMachinePrice() != null) {
                    machineAmount = listing.getMachinePrice().multiply(BigDecimal.valueOf(hours));
                } else if (listing.getRate() != null) {
                    machineAmount = BigDecimal.valueOf(listing.getRate() * hours);
                }
                
                // Driver pricing (if included and requested)
                if (dto.getIncludeDriver() && listing.getHasDriver() && listing.getDriverPrice() != null) {
                    driverAmount = listing.getDriverPrice().multiply(BigDecimal.valueOf(hours));
                }
                break;
                
            case MANUAL:
                // Labor pricing
                int workers = dto.getPeopleCount() != null ? dto.getPeopleCount() : 1;
                if (listing.getRate() != null) {
                    labourAmount = BigDecimal.valueOf(listing.getRate() * hours * workers);
                }
                break;
                
            case IRRIGATION:
            case SPRAYER:
            case TRANSPORT:
                // Standard rate-based pricing
                if (listing.getRate() != null) {
                    machineAmount = BigDecimal.valueOf(listing.getRate() * hours);
                }
                break;
        }

        booking.setMachineAmount(machineAmount);
        booking.setDriverAmount(driverAmount);
        booking.setLabourAmount(labourAmount);
        booking.setPlatformFeePercentage(platformFeePercentage);
        
        // Calculate totals
        booking.calculateTotals();
    }

    /**
     * Process payment for a booking (called after Razorpay payment success).
     */
    @Transactional
    public ServiceBookingResponseDto processBookingPayment(Long bookingId, String razorpayOrderId, 
                                                           String razorpayPaymentId, String paymentMethod) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        booking.setRazorpayOrderId(razorpayOrderId);
        booking.setRazorpayPaymentId(razorpayPaymentId);
        booking.setPaymentMethod(paymentMethod);
        booking.setPaidAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.CONFIRMED);

        ServiceBooking saved = serviceBookingRepository.save(booking);

        try {
            notificationService.createForUser(
                booking.getUser(),
                NotificationType.PAYMENT,
                "Service payment successful",
                "Payment received for booking #" + saved.getId() + ".",
                "/irrigation-services",
                NotificationPriority.NORMAL
            );
            if (booking.getProvider() != null) {
                notificationService.createForUser(
                    booking.getProvider(),
                    NotificationType.PAYMENT,
                    "Booking payment received",
                    "A booking payment was completed for your service.",
                    "/irrigation-services",
                    NotificationPriority.NORMAL
                );
            }
        } catch (Exception ignored) {
        }

        return toEnhancedBookingDto(saved);
    }

    /**
     * Complete a booking (triggers payout to provider).
     */
    @Transactional
    public ServiceBookingResponseDto completeBooking(Long bookingId, User user) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        // Only buyer or provider can complete
        boolean isBuyer = booking.getUser().getId().equals(user.getId());
        boolean isProvider = booking.getProvider() != null && booking.getProvider().getId().equals(user.getId());
        
        if (!isBuyer && !isProvider) {
            throw new UnauthorizedException("You are not authorized to complete this booking");
        }

        if (booking.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Payment must be completed before marking booking as complete");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());

        ServiceBooking saved = serviceBookingRepository.save(booking);

        try {
            notificationService.createForUser(
                booking.getUser(),
                NotificationType.PRODUCT,
                "Service booking completed",
                "Booking #" + saved.getId() + " has been marked completed.",
                "/irrigation-services",
                NotificationPriority.NORMAL
            );
            if (booking.getProvider() != null) {
                notificationService.createForUser(
                    booking.getProvider(),
                    NotificationType.PRODUCT,
                    "Service completed",
                    "Booking #" + saved.getId() + " was completed by the user.",
                    "/irrigation-services",
                    NotificationPriority.NORMAL
                );
            }
        } catch (Exception ignored) {
        }

        // Send Service Completed SMS
        try {
            String userPhone = booking.getUser().getPhone();
            if (userPhone != null && !userPhone.isBlank()) {
                smsService.sendServiceCompleted(
                    userPhone,
                    booking.getUser().getUsername(),
                    String.valueOf(booking.getId())
                );
            }
        } catch (Exception smsEx) {
            System.err.println("Failed to send service completed SMS: " + smsEx.getMessage());
        }

        return toEnhancedBookingDto(saved);
    }

    /**
     * Get price breakdown for a service listing.
     */
    public Map<String, Object> getPriceBreakdown(Long listingId, Integer hours, Boolean includeDriver, Integer workers) {
        ServiceListing listing = serviceListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service listing not found"));

        Map<String, Object> breakdown = new HashMap<>();
        BigDecimal machineAmount = BigDecimal.ZERO;
        BigDecimal driverAmount = BigDecimal.ZERO;
        BigDecimal labourAmount = BigDecimal.ZERO;

        int h = hours != null ? hours : 1;
        int w = workers != null ? workers : 1;

        switch (listing.getType()) {
            case TRACTOR:
            case JCB:
            case HARVESTER:
                if (listing.getMachinePrice() != null) {
                    machineAmount = listing.getMachinePrice().multiply(BigDecimal.valueOf(h));
                } else if (listing.getRate() != null) {
                    machineAmount = BigDecimal.valueOf(listing.getRate() * h);
                }
                if (includeDriver != null && includeDriver && listing.getHasDriver() && listing.getDriverPrice() != null) {
                    driverAmount = listing.getDriverPrice().multiply(BigDecimal.valueOf(h));
                }
                break;
            case MANUAL:
                if (listing.getRate() != null) {
                    labourAmount = BigDecimal.valueOf(listing.getRate() * h * w);
                }
                break;
            default:
                if (listing.getRate() != null) {
                    machineAmount = BigDecimal.valueOf(listing.getRate() * h);
                }
        }

        BigDecimal subtotal = machineAmount.add(driverAmount).add(labourAmount);
        BigDecimal platformFee = subtotal.multiply(platformFeePercentage).divide(BigDecimal.valueOf(100));
        BigDecimal total = subtotal;

        breakdown.put("machineAmount", machineAmount);
        breakdown.put("driverAmount", driverAmount);
        breakdown.put("labourAmount", labourAmount);
        breakdown.put("subtotal", subtotal);
        breakdown.put("platformFee", platformFee);
        breakdown.put("platformFeePercentage", platformFeePercentage);
        breakdown.put("total", total);
        breakdown.put("providerPayout", subtotal.subtract(platformFee));
        breakdown.put("hasDriver", listing.getHasDriver());
        breakdown.put("driverPricePerHour", listing.getDriverPrice());
        breakdown.put("machinePricePerHour", listing.getMachinePrice());
        breakdown.put("priceUnit", listing.getPriceUnit() != null ? listing.getPriceUnit().name() : "PER_HOUR");

        return breakdown;
    }

    /**
     * Convert ServiceListing to enhanced DTO.
     */
    private ServiceListingResponseDto toEnhancedDto(ServiceListing entity) {
        ServiceListingResponseDto dto = new ServiceListingResponseDto();
        dto.setId(entity.getId());
        dto.setType(entity.getType() != null ? entity.getType().name() : null);
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setLocation(entity.getLocation());
        dto.setRate(entity.getRate());
        
        // Enhanced pricing
        dto.setBasePrice(entity.getBasePrice());
        dto.setHasDriver(entity.getHasDriver());
        dto.setDriverPrice(entity.getDriverPrice());
        dto.setMachinePrice(entity.getMachinePrice());
        dto.setTotalPrice(entity.calculateTotalPrice());
        dto.setPriceUnit(entity.getPriceUnit() != null ? entity.getPriceUnit().name() : null);
        
        // Equipment details
        dto.setFuelIncluded(entity.getFuelIncluded());
        dto.setOperatorIncluded(entity.getOperatorIncluded());
        dto.setMinimumHours(entity.getMinimumHours());
        dto.setMaximumHours(entity.getMaximumHours());
        dto.setServiceRadiusKm(entity.getServiceRadiusKm());
        dto.setEquipmentPower(entity.getEquipmentPower());
        dto.setEquipmentModel(entity.getEquipmentModel());
        
        // Parse implements
        if (entity.getImplementsAvailable() != null) {
            try {
                List<String> implements_ = objectMapper.readValue(entity.getImplementsAvailable(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                dto.setImplementsAvailable(implements_);
            } catch (JsonProcessingException e) {
                dto.setImplementsAvailable(Arrays.asList(entity.getImplementsAvailable().split(",")));
            }
        }
        
        // Manual labor
        dto.setWorkersCount(entity.getWorkersCount());
        dto.setToolsIncluded(entity.getToolsIncluded());
        dto.setExperienceYears(entity.getExperienceYears());
        
        // Status
        dto.setIsActive(entity.getIsActive());
        dto.setPayoutStatus(entity.getPayoutStatus() != null ? entity.getPayoutStatus().name() : null);
        
        // Contact
        dto.setContactName(entity.getContactName());
        dto.setContactPhone(entity.getContactPhone());
        dto.setContactEmail(entity.getContactEmail());
        dto.setAvailability(entity.getAvailability());
        
        // Provider
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserName(entity.getUser().getUsername());
            dto.setUserFullName(entity.getUser().getUsername());
        }
        
        // Custom attributes
        List<ServiceAttribute> attributes = serviceAttributeRepository.findByServiceListingIdOrderByDisplayOrderAsc(entity.getId());
        if (attributes != null && !attributes.isEmpty()) {
            Map<String, Object> customAttrs = new HashMap<>();
            for (ServiceAttribute attr : attributes) {
                customAttrs.put(attr.getAttributeKey(), attr.getAttributeValue());
            }
            dto.setCustomAttributes(customAttrs);
        }
        
        // Timestamps
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(formatter));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().format(formatter));
        }

        return dto;
    }

    /**
     * Convert ServiceBooking to enhanced DTO.
     */
    private ServiceBookingResponseDto toEnhancedBookingDto(ServiceBooking booking) {
        ServiceBookingResponseDto dto = new ServiceBookingResponseDto();
        dto.setId(booking.getId());
        dto.setServiceType(booking.getServiceType() != null ? booking.getServiceType().name() : null);
        dto.setLocation(booking.getLocation());
        dto.setHours(booking.getHours());
        dto.setPeopleCount(booking.getPeopleCount());
        dto.setNotes(booking.getNotes());
        dto.setStatus(booking.getStatus() != null ? booking.getStatus().name() : null);

        // User (buyer)
        if (booking.getUser() != null) {
            dto.setUserId(booking.getUser().getId());
            dto.setUserName(booking.getUser().getUsername());
            dto.setUserPhone(booking.getUser().getPhone());
        }

        // Provider (seller)
        if (booking.getProvider() != null) {
            dto.setProviderId(booking.getProvider().getId());
            dto.setProviderName(booking.getProvider().getUsername());
            dto.setProviderPhone(booking.getProvider().getPhone());
        }

        // Service listing
        if (booking.getServiceListing() != null) {
            dto.setServiceListingId(booking.getServiceListing().getId());
            dto.setServiceTitle(booking.getServiceListing().getTitle());
        }

        // Farm and crop
        if (booking.getFarm() != null) {
            dto.setFarmId(booking.getFarm().getId());
            dto.setFarmName(booking.getFarm().getFarmName());
        }
        if (booking.getCrop() != null) {
            dto.setCropId(booking.getCrop().getId());
            dto.setCropName(booking.getCrop().getCropName());
        }

        // Scheduling
        dto.setBookingDate(booking.getBookingDate());
        dto.setServiceDate(booking.getServiceDate());
        dto.setStartTime(booking.getStartTime());
        dto.setEndTime(booking.getEndTime());

        // Pricing
        dto.setMachineAmount(booking.getMachineAmount());
        dto.setDriverAmount(booking.getDriverAmount());
        dto.setLabourAmount(booking.getLabourAmount());
        dto.setSubtotalAmount(booking.getSubtotalAmount());
        dto.setPlatformFee(booking.getPlatformFee());
        dto.setPlatformFeePercentage(booking.getPlatformFeePercentage());
        dto.setTaxAmount(booking.getTaxAmount());
        dto.setTotalAmount(booking.getTotalAmount());

        // Payment
        dto.setPaymentStatus(booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : null);
        dto.setPaymentMethod(booking.getPaymentMethod());
        dto.setTransactionId(booking.getTransactionId());
        dto.setRazorpayOrderId(booking.getRazorpayOrderId());
        dto.setRazorpayPaymentId(booking.getRazorpayPaymentId());
        if (booking.getPaidAt() != null) {
            dto.setPaidAt(booking.getPaidAt().format(formatter));
        }

        // Payout
        dto.setPayoutStatus(booking.getPayoutStatus() != null ? booking.getPayoutStatus().name() : null);
        dto.setPayoutAmount(booking.getPayoutAmount());
        dto.setPayoutTransactionId(booking.getPayoutTransactionId());
        if (booking.getPayoutAt() != null) {
            dto.setPayoutAt(booking.getPayoutAt().format(formatter));
        }

        // Cancellation
        dto.setCancellationReason(booking.getCancellationReason());
        if (booking.getCancelledAt() != null) {
            dto.setCancelledAt(booking.getCancelledAt().format(formatter));
        }
        if (booking.getCompletedAt() != null) {
            dto.setCompletedAt(booking.getCompletedAt().format(formatter));
        }

        // Rating
        dto.setRating(booking.getRating());
        dto.setReview(booking.getReview());

        // Timestamps
        if (booking.getCreatedAt() != null) {
            dto.setCreatedAt(booking.getCreatedAt().format(formatter));
        }
        if (booking.getUpdatedAt() != null) {
            dto.setUpdatedAt(booking.getUpdatedAt().format(formatter));
        }

        return dto;
    }

    private ServiceBookingDto toBookingDto(ServiceBooking booking) {
        ServiceBookingDto dto = new ServiceBookingDto();
        dto.setId(booking.getId());
        dto.setServiceType(booking.getServiceType());
        dto.setLocation(booking.getLocation());
        dto.setHours(booking.getHours());
        dto.setPeopleCount(booking.getPeopleCount());
        dto.setNotes(booking.getNotes());
        dto.setStatus(booking.getStatus());

        if (booking.getUser() != null) {
            dto.setCustomerId(booking.getUser().getId());
            dto.setCustomerName(booking.getUser().getUsername());
        }

        if (booking.getServiceListing() != null) {
            dto.setServiceListingId(booking.getServiceListing().getId());
            if (booking.getServiceListing().getUser() != null) {
                dto.setProviderId(booking.getServiceListing().getUser().getId());
                dto.setProviderName(booking.getServiceListing().getUser().getUsername());
            }
        }

        if (booking.getFarm() != null) {
            dto.setFarmId(booking.getFarm().getId());
            dto.setFarmName(booking.getFarm().getFarmName());
        }

        if (booking.getCrop() != null) {
            dto.setCropId(booking.getCrop().getId());
            dto.setCropName(booking.getCrop().getCropName());
        }

        return dto;
    }
}

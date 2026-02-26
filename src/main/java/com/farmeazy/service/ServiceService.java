package com.farmeazy.service;


import com.farmeazy.dto.ServiceBookingDto;
import com.farmeazy.dto.ServiceListingDto;
import com.farmeazy.entity.Crop;
import com.farmeazy.entity.Farm;
import com.farmeazy.entity.ServiceBooking;
import com.farmeazy.entity.ServiceBooking.BookingStatus;
import com.farmeazy.entity.ServiceListing;
import com.farmeazy.entity.User;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.repository.CropRepository;
import com.farmeazy.repository.FarmRepository;
import com.farmeazy.repository.ServiceBookingRepository;
import com.farmeazy.repository.ServiceListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceService {

    @Autowired
    private HttpEmailService httpEmailService;

    @Autowired
    private ServiceListingRepository serviceListingRepository;

    @Autowired
    private ServiceBookingRepository serviceBookingRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private CropRepository cropRepository;

    @Transactional
    public ServiceListingDto createServiceListing(User user, ServiceListingDto dto) {
        ServiceListing entity = new ServiceListing();
        entity.setTitle(dto.getServiceName());
        entity.setUser(user);
        entity.setRate(dto.getPrice());
        entity.setContactName(dto.getContactName() != null ? dto.getContactName() : user.getFullName());
        entity.setContactPhone(dto.getContactPhone() != null ? dto.getContactPhone() : user.getPhone());
        entity.setContactEmail(dto.getContactEmail() != null ? dto.getContactEmail() : user.getEmail());
        entity.setDescription(dto.getDescription());
        entity.setType(dto.getType());
        entity.setLocation(dto.getLocation());
        entity.setAvailability(dto.getAvailability() != null ? dto.getAvailability() : "Available");

        ServiceListing saved = serviceListingRepository.save(entity);

        // Send service listing creation email notification
        try {
            httpEmailService.sendServiceListingCreatedNotification(
                user.getEmail(),
                user.getFullName(),
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

        ServiceListing updated = serviceListingRepository.save(entity);

        // Send service listing update email notification
        try {
            httpEmailService.sendServiceListingUpdatedNotification(
                user.getEmail(),
                user.getFullName(),
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

        // Send service listing deletion email notification
        try {
            httpEmailService.sendServiceListingDeletedNotification(
                user.getEmail(),
                user.getFullName(),
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
        dto.setDescription(entity.getDescription());
        dto.setType(entity.getType());
        dto.setLocation(entity.getLocation());
        dto.setAvailability(entity.getAvailability());
        dto.setContactName(entity.getContactName());
        dto.setContactPhone(entity.getContactPhone());
        dto.setContactEmail(entity.getContactEmail());
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

        // Send email notifications
        try {
            httpEmailService.sendServiceBookingApprovedNotification(
                    booking.getUser().getEmail(),
                    booking.getUser().getFullName(),
                    booking.getServiceType().toString(),
                    booking.getLocation(),
                    provider.getFullName()
            );
        } catch (Exception e) {
            System.err.println("Failed to send booking approval email: " + e.getMessage());
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

        // Send email notifications
        try {
            httpEmailService.sendServiceBookingDeclinedNotification(
                    booking.getUser().getEmail(),
                    booking.getUser().getFullName(),
                    booking.getServiceType().toString(),
                    booking.getLocation(),
                    provider.getFullName()
            );
        } catch (Exception e) {
            System.err.println("Failed to send booking decline email: " + e.getMessage());
        }

        return toBookingDto(updated);
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
            dto.setCustomerName(booking.getUser().getFullName());
        }

        if (booking.getServiceListing() != null) {
            dto.setServiceListingId(booking.getServiceListing().getId());
            if (booking.getServiceListing().getUser() != null) {
                dto.setProviderId(booking.getServiceListing().getUser().getId());
                dto.setProviderName(booking.getServiceListing().getUser().getFullName());
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

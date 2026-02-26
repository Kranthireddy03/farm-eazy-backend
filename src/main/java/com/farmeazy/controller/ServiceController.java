package com.farmeazy.controller;

import com.farmeazy.dto.ServiceBookingDto;
import com.farmeazy.dto.ServiceListingDto;
import com.farmeazy.entity.User;
import com.farmeazy.service.ServiceService;
import com.farmeazy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;

import java.security.Principal;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = {
    "https://farm-eazy.com",
    "https://www.farm-eazy.com",
    "https://farm-eazy.vercel.app",
    "http://localhost:4200",
    "http://localhost:3000"
})
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private UserService userService;


    @PostMapping("/listings")
    public ResponseEntity<ServiceListingDto> createServiceListing(@Valid @RequestBody ServiceListingDto serviceListingDto, Principal principal) {
        User user = userService.findByEmail(principal.getName());
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
}

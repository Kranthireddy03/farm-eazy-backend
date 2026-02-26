package com.farmeazy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "service_bookings")
public class ServiceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceListing.ServiceType serviceType;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Integer hours;

    @Column(name = "people_count")
    private Integer peopleCount; // Only for manual labor

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // The person requesting the service

    @ManyToOne
    @JoinColumn(name = "service_listing_id")
    private ServiceListing serviceListing; // The specific service being booked

    @ManyToOne
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne
    @JoinColumn(name = "crop_id")
    private Crop crop;

    public enum BookingStatus {
        PENDING,
        APPROVED,
        DECLINED,
        COMPLETED,
        CANCELLED
    }

    public ServiceBooking() {
    }

    public ServiceBooking(ServiceListing.ServiceType serviceType, String location, Integer hours, Integer peopleCount, String notes, User user, Farm farm, Crop crop) {
        this.serviceType = serviceType;
        this.location = location;
        this.hours = hours;
        this.peopleCount = peopleCount;
        this.notes = notes;
        this.user = user;
        this.farm = farm;
        this.crop = crop;
        this.status = BookingStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceListing.ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceListing.ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getHours() {
        return hours;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ServiceListing getServiceListing() {
        return serviceListing;
    }

    public void setServiceListing(ServiceListing serviceListing) {
        this.serviceListing = serviceListing;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public Crop getCrop() {
        return crop;
    }

    public void setCrop(Crop crop) {
        this.crop = crop;
    }
}

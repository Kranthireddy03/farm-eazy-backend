package com.farmeazy.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Enhanced ServiceBooking entity for marketplace with escrow payment.
 * Tracks booking details, payment status, and payout to service provider.
 */
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
    private User user; // The person requesting the service (buyer)

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private User provider; // The service provider (seller)

    @ManyToOne
    @JoinColumn(name = "service_listing_id")
    private ServiceListing serviceListing; // The specific service being booked

    @ManyToOne
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne
    @JoinColumn(name = "crop_id")
    private Crop crop;

    // Scheduling
    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    // Pricing breakdown
    @Column(name = "machine_amount", precision = 10, scale = 2)
    private BigDecimal machineAmount = BigDecimal.ZERO;

    @Column(name = "driver_amount", precision = 10, scale = 2)
    private BigDecimal driverAmount = BigDecimal.ZERO;

    @Column(name = "labour_amount", precision = 10, scale = 2)
    private BigDecimal labourAmount = BigDecimal.ZERO;

    @Column(name = "subtotal_amount", precision = 10, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "platform_fee", precision = 10, scale = 2)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "platform_fee_percentage", precision = 5, scale = 2)
    private BigDecimal platformFeePercentage = new BigDecimal("5.00");

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Payment details
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Payout to service provider
    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", length = 50)
    private PayoutStatus payoutStatus = PayoutStatus.PENDING;

    @Column(name = "payout_amount", precision = 10, scale = 2)
    private BigDecimal payoutAmount = BigDecimal.ZERO;

    @Column(name = "payout_transaction_id")
    private String payoutTransactionId;

    @Column(name = "payout_at")
    private LocalDateTime payoutAt;

    // Cancellation
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Rating and review
    @Column(name = "rating")
    private Integer rating;

    @Column(name = "review", columnDefinition = "TEXT")
    private String review;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum BookingStatus {
        PENDING,
        APPROVED,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED,
        DECLINED,
        CANCELLED
    }

    public enum PaymentStatus {
        PENDING,
        INITIATED,
        SUCCESS,
        FAILED,
        REFUNDED
    }

    public enum PayoutStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        NOT_APPLICABLE
    }

    public ServiceBooking() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        bookingDate = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate total amount from components
     */
    public void calculateTotals() {
        // Calculate subtotal
        subtotalAmount = machineAmount.add(driverAmount).add(labourAmount);
        
        // Calculate platform fee
        if (platformFeePercentage != null) {
            platformFee = subtotalAmount.multiply(platformFeePercentage).divide(new BigDecimal("100"));
        }
        
        // Calculate total (subtotal + tax, platform fee is deducted from provider's share)
        totalAmount = subtotalAmount.add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        
        // Payout amount is subtotal minus platform fee
        payoutAmount = subtotalAmount.subtract(platformFee);
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

    // New getters and setters for enhanced fields
    public User getProvider() {
        return provider;
    }

    public void setProvider(User provider) {
        this.provider = provider;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getMachineAmount() {
        return machineAmount;
    }

    public void setMachineAmount(BigDecimal machineAmount) {
        this.machineAmount = machineAmount;
    }

    public BigDecimal getDriverAmount() {
        return driverAmount;
    }

    public void setDriverAmount(BigDecimal driverAmount) {
        this.driverAmount = driverAmount;
    }

    public BigDecimal getLabourAmount() {
        return labourAmount;
    }

    public void setLabourAmount(BigDecimal labourAmount) {
        this.labourAmount = labourAmount;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getPlatformFeePercentage() {
        return platformFeePercentage;
    }

    public void setPlatformFeePercentage(BigDecimal platformFeePercentage) {
        this.platformFeePercentage = platformFeePercentage;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public PayoutStatus getPayoutStatus() {
        return payoutStatus;
    }

    public void setPayoutStatus(PayoutStatus payoutStatus) {
        this.payoutStatus = payoutStatus;
    }

    public BigDecimal getPayoutAmount() {
        return payoutAmount;
    }

    public void setPayoutAmount(BigDecimal payoutAmount) {
        this.payoutAmount = payoutAmount;
    }

    public String getPayoutTransactionId() {
        return payoutTransactionId;
    }

    public void setPayoutTransactionId(String payoutTransactionId) {
        this.payoutTransactionId = payoutTransactionId;
    }

    public LocalDateTime getPayoutAt() {
        return payoutAt;
    }

    public void setPayoutAt(LocalDateTime payoutAt) {
        this.payoutAt = payoutAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

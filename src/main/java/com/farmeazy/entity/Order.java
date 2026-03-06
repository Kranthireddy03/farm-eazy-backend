package com.farmeazy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display Order ID - Human readable format (OD00001, OD00002, etc.)
     * Auto-generated on order creation
     */
    @Column(name = "display_id", unique = true, length = 10)
    private String displayId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = true)
    private Address shippingAddress;

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private Long coinsUsed = 0L;
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime estimatedDeliveryDate;

    // Marketplace / Payout fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Column(name = "platform_fee", precision = 10, scale = 2)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "platform_fee_percentage", precision = 5, scale = 2)
    private BigDecimal platformFeePercentage = new BigDecimal("5.00");

    @Column(name = "seller_amount", precision = 10, scale = 2)
    private BigDecimal sellerAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_status", length = 50)
    private PayoutStatus payoutStatus = PayoutStatus.PENDING;

    @Column(name = "payout_transaction_id")
    private String payoutTransactionId;

    @Column(name = "payout_at")
    private LocalDateTime payoutAt;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    // Refund tracking fields
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 30)
    private RefundStatus refundStatus = RefundStatus.NOT_REQUESTED;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", length = 20)
    private RefundType refundType;

    @Column(name = "refund_requested_at")
    private LocalDateTime refundRequestedAt;

    @Column(name = "refund_approved_at")
    private LocalDateTime refundApprovedAt;

    @Column(name = "refund_completed_at")
    private LocalDateTime refundCompletedAt;

    @Column(name = "razorpay_refund_id", length = 100)
    private String razorpayRefundId;

    @Column(name = "refund_admin_notes", length = 500)
    private String refundAdminNotes;

    @Column(name = "refund_attempts")
    private Integer refundAttempts = 0;

    @Column(name = "refund_error_message", length = 500)
    private String refundErrorMessage;

    @Column(name = "cancellation_deadline")
    private LocalDateTime cancellationDeadline;

    @Column(name = "return_deadline")
    private LocalDateTime returnDeadline;

    // Coins refund tracking (separate from money refund)
    @Column(name = "coins_refunded")
    private Long coinsRefunded = 0L;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Order() {
    }

    public Order(User user, List<OrderItem> items, Address shippingAddress, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal totalAmount, Long coinsUsed, BigDecimal finalAmount, PaymentMethod paymentMethod, PaymentStatus paymentStatus, OrderStatus orderStatus, String transactionId, LocalDateTime paidAt, LocalDateTime estimatedDeliveryDate) {
        this.user = user;
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.coinsUsed = coinsUsed;
        this.finalAmount = finalAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.transactionId = transactionId;
        this.paidAt = paidAt;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayId() {
        return displayId;
    }

    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    /**
     * Generate display ID in format OD00001
     */
    public void generateDisplayId() {
        if (this.id != null && this.displayId == null) {
            this.displayId = String.format("OD%05d", this.id);
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
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

    public Long getCoinsUsed() {
        return coinsUsed;
    }

    public void setCoinsUsed(Long coinsUsed) {
        this.coinsUsed = coinsUsed;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
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

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.estimatedDeliveryDate == null) {
            this.estimatedDeliveryDate = LocalDateTime.now().plusDays(3);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        PENDING,      // Order placed, awaiting payment
        CONFIRMED,    // Payment received, processing
        SHIPPED,      // Order dispatched
        DELIVERED,    // Delivered to customer
        CANCELLED     // Order cancelled
    }

    public enum PaymentStatus {
        PENDING,      // Awaiting payment
        PROCESSING,   // Payment in progress
        COMPLETED,    // Payment successful
        FAILED,       // Payment failed
        CANCELLED,    // Payment cancelled
        REFUND_INITIATED  // Refund process started
    }

    public enum PaymentMethod {
        UPI,
        PHONEPAY,
        CARD,
        CASH_ON_DELIVERY,
        RAZORPAY
    }

    public enum PayoutStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        NOT_APPLICABLE
    }

    public enum RefundStatus {
        NOT_REQUESTED,           // Default, no refund action taken
        REQUESTED,               // User has requested refund/cancellation
        REFUND_DETAILS_REQUIRED, // User needs to add bank/UPI details
        PENDING_APPROVAL,        // Awaiting admin approval (for high-value orders)
        APPROVED,                // Approved, waiting to process via Razorpay
        PROCESSING,              // Refund initiated with Razorpay
        COMPLETED,               // Refund successfully credited to user
        FAILED,                  // Refund attempt failed (will retry)
        REJECTED,                // Refund not allowed (policy violation)
        PARTIALLY_REFUNDED       // Partial amount refunded
    }

    public enum RefundType {
        CANCELLATION,    // Order cancelled before shipping
        RETURN,          // Product returned after delivery
        PARTIAL_RETURN   // Some items returned from order
    }

    // Getters and Setters for new marketplace fields
    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
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

    public BigDecimal getSellerAmount() {
        return sellerAmount;
    }

    public void setSellerAmount(BigDecimal sellerAmount) {
        this.sellerAmount = sellerAmount;
    }

    public PayoutStatus getPayoutStatus() {
        return payoutStatus;
    }

    public void setPayoutStatus(PayoutStatus payoutStatus) {
        this.payoutStatus = payoutStatus;
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

    /**
     * Calculate seller payout amount (total - platform fee).
     */
    public void calculateSellerPayout() {
        if (totalAmount != null && platformFeePercentage != null) {
            this.platformFee = totalAmount.multiply(platformFeePercentage).divide(new BigDecimal("100"));
            this.sellerAmount = totalAmount.subtract(platformFee);
        }
    }

    // Refund field getters and setters
    public RefundStatus getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(RefundStatus refundStatus) {
        this.refundStatus = refundStatus;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public RefundType getRefundType() {
        return refundType;
    }

    public void setRefundType(RefundType refundType) {
        this.refundType = refundType;
    }

    public LocalDateTime getRefundRequestedAt() {
        return refundRequestedAt;
    }

    public void setRefundRequestedAt(LocalDateTime refundRequestedAt) {
        this.refundRequestedAt = refundRequestedAt;
    }

    public LocalDateTime getRefundApprovedAt() {
        return refundApprovedAt;
    }

    public void setRefundApprovedAt(LocalDateTime refundApprovedAt) {
        this.refundApprovedAt = refundApprovedAt;
    }

    public LocalDateTime getRefundCompletedAt() {
        return refundCompletedAt;
    }

    public void setRefundCompletedAt(LocalDateTime refundCompletedAt) {
        this.refundCompletedAt = refundCompletedAt;
    }

    public String getRazorpayRefundId() {
        return razorpayRefundId;
    }

    public void setRazorpayRefundId(String razorpayRefundId) {
        this.razorpayRefundId = razorpayRefundId;
    }

    public String getRefundAdminNotes() {
        return refundAdminNotes;
    }

    public void setRefundAdminNotes(String refundAdminNotes) {
        this.refundAdminNotes = refundAdminNotes;
    }

    public Integer getRefundAttempts() {
        return refundAttempts;
    }

    public void setRefundAttempts(Integer refundAttempts) {
        this.refundAttempts = refundAttempts;
    }

    public String getRefundErrorMessage() {
        return refundErrorMessage;
    }

    public void setRefundErrorMessage(String refundErrorMessage) {
        this.refundErrorMessage = refundErrorMessage;
    }

    public LocalDateTime getCancellationDeadline() {
        return cancellationDeadline;
    }

    public void setCancellationDeadline(LocalDateTime cancellationDeadline) {
        this.cancellationDeadline = cancellationDeadline;
    }

    public LocalDateTime getReturnDeadline() {
        return returnDeadline;
    }

    public void setReturnDeadline(LocalDateTime returnDeadline) {
        this.returnDeadline = returnDeadline;
    }

    public Long getCoinsRefunded() {
        return coinsRefunded;
    }

    public void setCoinsRefunded(Long coinsRefunded) {
        this.coinsRefunded = coinsRefunded;
    }

    /**
     * Check if order can be cancelled (within cancellation window).
     */
    public boolean canBeCancelled() {
        if (orderStatus == OrderStatus.SHIPPED || orderStatus == OrderStatus.DELIVERED || orderStatus == OrderStatus.CANCELLED) {
            return false;
        }
        if (refundStatus != RefundStatus.NOT_REQUESTED) {
            return false; // Already requested
        }
        if (cancellationDeadline != null && LocalDateTime.now().isAfter(cancellationDeadline)) {
            return false;
        }
        return true;
    }

    /**
     * Check if order can be returned (within return window).
     */
    public boolean canBeReturned() {
        if (orderStatus != OrderStatus.DELIVERED) {
            return false;
        }
        if (refundStatus != RefundStatus.NOT_REQUESTED) {
            return false; // Already requested
        }
        if (returnDeadline != null && LocalDateTime.now().isAfter(returnDeadline)) {
            return false;
        }
        return true;
    }

    /**
     * Calculate refund amount (full order amount for now, partial refunds can be added later).
     */
    public void calculateRefundAmount() {
        this.refundAmount = this.finalAmount != null ? this.finalAmount : this.totalAmount;
    }
}

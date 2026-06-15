package com.farmeazy.service;

import com.farmeazy.dto.PayoutDto;
import com.farmeazy.entity.*;
import com.farmeazy.exception.ResourceNotFoundException;
import com.farmeazy.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling payouts to sellers.
 * Implements escrow model: Buyer pays -> FarmEazy holds -> Payout to Seller.
 */
@Service
public class PayoutService {

    private static final Logger logger = LoggerFactory.getLogger(PayoutService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PayoutRepository payoutRepository;
    private final UserBankDetailsRepository bankDetailsRepository;
    private final ServiceBookingRepository serviceBookingRepository;
    private final OrderRepository orderRepository;
    private final PlatformWalletRepository platformWalletRepository;
    private final UserRepository userRepository;

    @Value("${farmeazy.platform.fee.percentage:5.00}")
    private BigDecimal defaultPlatformFeePercentage;

    @Autowired
    public PayoutService(PayoutRepository payoutRepository,
                        UserBankDetailsRepository bankDetailsRepository,
                        ServiceBookingRepository serviceBookingRepository,
                        OrderRepository orderRepository,
                        PlatformWalletRepository platformWalletRepository,
                        UserRepository userRepository) {
        this.payoutRepository = payoutRepository;
        this.bankDetailsRepository = bankDetailsRepository;
        this.serviceBookingRepository = serviceBookingRepository;
        this.orderRepository = orderRepository;
        this.platformWalletRepository = platformWalletRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a payout record for a service booking after payment success.
     */
    @Transactional
    @CacheEvict(cacheNames = {"payoutListUser", "payoutPending"}, allEntries = true)
    public Payout createServiceBookingPayout(ServiceBooking booking) {
        // Check if payout already exists
        if (payoutRepository.existsByReferenceTypeAndReferenceId(
                Payout.ReferenceType.SERVICE_BOOKING, booking.getId())) {
            logger.warn("Payout already exists for service booking: {}", booking.getId());
            return null;
        }

        User provider = booking.getProvider();
        if (provider == null && booking.getServiceListing() != null) {
            provider = booking.getServiceListing().getUser();
        }

        if (provider == null) {
            logger.error("No provider found for service booking: {}", booking.getId());
            return null;
        }

        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(provider.getId())
                .orElse(null);

        if (bankDetails == null) {
            logger.warn("No bank details found for provider: {}. Payout pending bank details.", provider.getId());
            // Still create payout record but mark as pending
        }

        Payout payout = new Payout();
        payout.setUser(provider);
        payout.setBankDetail(bankDetails);
        payout.setReferenceType(Payout.ReferenceType.SERVICE_BOOKING);
        payout.setReferenceId(booking.getId());
        payout.setGrossAmount(booking.getSubtotalAmount());
        payout.setPlatformFee(booking.getPlatformFee());
        payout.setNetAmount(booking.getPayoutAmount());
        payout.setStatus(bankDetails != null ? Payout.PayoutStatus.PENDING : Payout.PayoutStatus.PENDING);

        Payout saved = payoutRepository.save(payout);

        // Record platform earning
        recordPlatformEarning(Payout.ReferenceType.SERVICE_BOOKING, booking.getId(), 
                             booking.getPlatformFee(), "Platform fee from service booking #" + booking.getId());

        logger.info("Created payout for service booking: {} Amount: {}", booking.getId(), payout.getNetAmount());
        return saved;
    }

    /**
     * Create a payout record for a product order after payment success.
     */
    @Transactional
    @CacheEvict(cacheNames = {"payoutListUser", "payoutPending"}, allEntries = true)
    public Payout createProductOrderPayout(Order order, User seller, BigDecimal sellerAmount, BigDecimal platformFee) {
        // Check if payout already exists
        if (payoutRepository.existsByReferenceTypeAndReferenceId(
                Payout.ReferenceType.PRODUCT_ORDER, order.getId())) {
            logger.warn("Payout already exists for product order: {}", order.getId());
            return null;
        }

        UserBankDetails bankDetails = bankDetailsRepository.findByUserId(seller.getId())
                .orElse(null);

        Payout payout = new Payout();
        payout.setUser(seller);
        payout.setBankDetail(bankDetails);
        payout.setReferenceType(Payout.ReferenceType.PRODUCT_ORDER);
        payout.setReferenceId(order.getId());
        payout.setGrossAmount(sellerAmount.add(platformFee));
        payout.setPlatformFee(platformFee);
        payout.setNetAmount(sellerAmount);
        payout.setStatus(Payout.PayoutStatus.PENDING);

        Payout saved = payoutRepository.save(payout);

        // Record platform earning
        recordPlatformEarning(Payout.ReferenceType.PRODUCT_ORDER, order.getId(), 
                             platformFee, "Platform fee from product order #" + order.getId());

        logger.info("Created payout for product order: {} Amount: {}", order.getId(), payout.getNetAmount());
        return saved;
    }

    /**
     * Process pending payouts (scheduled job).
     * Runs every 2 hours. In production, this would integrate with Razorpay Payout API.
     */
    @Scheduled(cron = "0 0 */2 * * *") // Every 2 hours
    @Transactional
    @CacheEvict(cacheNames = {"payoutListUser", "payoutPending", "payoutById"}, allEntries = true)
    public void processPendingPayouts() {
        LocalDateTime jobStartTime = LocalDateTime.now();
        logger.info("BATCH_JOB_START: Payout processing job started at {}", jobStartTime);
        
        List<Payout> pendingPayouts = payoutRepository.findByStatus(Payout.PayoutStatus.PENDING);
        int successCount = 0;
        int failCount = 0;
        
        for (Payout payout : pendingPayouts) {
            try {
                processSinglePayout(payout);
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to process payout {}: {}", payout.getId(), e.getMessage());
                payout.setStatus(Payout.PayoutStatus.FAILED);
                payout.setFailureReason(e.getMessage());
                payoutRepository.save(payout);
                failCount++;
            }
        }
        
        LocalDateTime jobEndTime = LocalDateTime.now();
        logger.info("BATCH_JOB_END: Payout processing completed at {}. Total: {}, Success: {}, Failed: {}", 
                   jobEndTime, pendingPayouts.size(), successCount, failCount);
    }

    /**
     * Process a single payout.
     * In production, this would call Razorpay Payout API.
     */
    @Transactional
    @CacheEvict(cacheNames = {"payoutListUser", "payoutPending", "payoutById"}, allEntries = true)
    public void processSinglePayout(Payout payout) {
        if (payout.getBankDetail() == null) {
            throw new RuntimeException("Bank details not found for user");
        }

        if (!payout.getBankDetail().getIsVerified()) {
            logger.warn("Bank details not verified for payout: {}", payout.getId());
            // In production, you might skip unverified accounts
        }

        payout.setStatus(Payout.PayoutStatus.PROCESSING);
        payoutRepository.save(payout);

        // TODO: Integrate with Razorpay Payout API
        // RazorpayClient client = new RazorpayClient(keyId, keySecret);
        // JSONObject payoutRequest = new JSONObject();
        // payoutRequest.put("account_number", "<your_account_number>");
        // payoutRequest.put("fund_account_id", fundAccountId);
        // payoutRequest.put("amount", payout.getNetAmount().multiply(new BigDecimal("100")).intValue());
        // payoutRequest.put("currency", "INR");
        // payoutRequest.put("mode", "IMPS");
        // Payout razorpayPayout = client.payouts.create(payoutRequest);

        // For now, simulate successful payout
        payout.setStatus(Payout.PayoutStatus.COMPLETED);
        payout.setProcessedAt(LocalDateTime.now());
        payout.setRazorpayPayoutId("SIMULATED_" + System.currentTimeMillis());
        payoutRepository.save(payout);

        // Update the booking/order payout status
        updateReferencePayoutStatus(payout);

        // Record platform debit
        recordPlatformDebit(payout);

        logger.info("Payout {} completed successfully", payout.getId());
    }

    /**
     * Update the payout status in the original booking/order.
     */
    private void updateReferencePayoutStatus(Payout payout) {
        if (payout.getReferenceType() == Payout.ReferenceType.SERVICE_BOOKING) {
            serviceBookingRepository.findById(payout.getReferenceId()).ifPresent(booking -> {
                booking.setPayoutStatus(ServiceBooking.PayoutStatus.COMPLETED);
                booking.setPayoutTransactionId(payout.getRazorpayPayoutId());
                booking.setPayoutAt(LocalDateTime.now());
                serviceBookingRepository.save(booking);
            });
        }
        // Add similar logic for PRODUCT_ORDER when needed
    }

    /**
     * Record platform earning in wallet.
     */
    private void recordPlatformEarning(Payout.ReferenceType referenceType, Long referenceId, 
                                       BigDecimal amount, String description) {
        BigDecimal currentBalance = platformWalletRepository.getCurrentBalance();
        if (currentBalance == null) currentBalance = BigDecimal.ZERO;

        PlatformWallet.ReferenceType walletRefType = referenceType == Payout.ReferenceType.SERVICE_BOOKING
                ? PlatformWallet.ReferenceType.SERVICE_BOOKING
                : PlatformWallet.ReferenceType.PRODUCT_ORDER;

        PlatformWallet wallet = new PlatformWallet();
        wallet.setTransactionType(PlatformWallet.TransactionType.CREDIT);
        wallet.setReferenceType(walletRefType);
        wallet.setReferenceId(referenceId);
        wallet.setAmount(amount);
        wallet.setDescription(description);
        wallet.setBalanceAfter(currentBalance.add(amount));
        platformWalletRepository.save(wallet);
    }

    /**
     * Record platform debit (payout to seller) in wallet.
     */
    private void recordPlatformDebit(Payout payout) {
        BigDecimal currentBalance = platformWalletRepository.getCurrentBalance();
        if (currentBalance == null) currentBalance = BigDecimal.ZERO;

        PlatformWallet wallet = new PlatformWallet();
        wallet.setTransactionType(PlatformWallet.TransactionType.DEBIT);
        wallet.setReferenceType(PlatformWallet.ReferenceType.PAYOUT);
        wallet.setReferenceId(payout.getId());
        wallet.setAmount(payout.getNetAmount());
        wallet.setDescription("Payout to seller for " + payout.getReferenceType() + " #" + payout.getReferenceId());
        wallet.setBalanceAfter(currentBalance.subtract(payout.getNetAmount()));
        platformWalletRepository.save(wallet);
    }

    /**
     * Get payouts for a user.
     */
    @Cacheable(cacheNames = "payoutListUser", key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<PayoutDto> getPayoutsByUserId(Long userId) {
        return payoutRepository.findByUser_Id(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all pending payouts (admin).
     */
    @Cacheable(cacheNames = "payoutPending", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<PayoutDto> getPendingPayouts() {
        return payoutRepository.findByStatus(Payout.PayoutStatus.PENDING).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get payout by ID.
     */
    @Cacheable(cacheNames = "payoutById", key = "#id", unless = "#result == null")
    public PayoutDto getPayoutById(Long id) {
        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found with ID: " + id));
        return toDto(payout);
    }

    /**
     * Get total earnings for a seller.
     */
    public BigDecimal getTotalEarnings(Long userId) {
        BigDecimal total = payoutRepository.getTotalPayoutsByUserId(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get total platform earnings.
     */
    public BigDecimal getTotalPlatformEarnings() {
        BigDecimal total = payoutRepository.getTotalPlatformEarnings();
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Convert entity to DTO.
     */
    private PayoutDto toDto(Payout entity) {
        PayoutDto dto = new PayoutDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUser().getId());
        dto.setUserName(entity.getUser().getUsername());
        if (entity.getBankDetail() != null) {
            dto.setBankDetailId(entity.getBankDetail().getId());
            dto.setMaskedAccountNumber(entity.getBankDetail().getMaskedAccountNumber());
            dto.setBankName(entity.getBankDetail().getBankName());
        }
        dto.setReferenceType(entity.getReferenceType().name());
        dto.setReferenceId(entity.getReferenceId());
        dto.setGrossAmount(entity.getGrossAmount());
        dto.setPlatformFee(entity.getPlatformFee());
        dto.setNetAmount(entity.getNetAmount());
        dto.setStatus(entity.getStatus().name());
        dto.setRazorpayPayoutId(entity.getRazorpayPayoutId());
        dto.setFailureReason(entity.getFailureReason());
        if (entity.getProcessedAt() != null) {
            dto.setProcessedAt(entity.getProcessedAt().format(formatter));
        }
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(formatter));
        }
        if (entity.getUpdatedAt() != null) {
            dto.setUpdatedAt(entity.getUpdatedAt().format(formatter));
        }
        return dto;
    }
}

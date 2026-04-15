package com.farmeazy.service;

import com.farmeazy.entity.BankVerificationRequest.VerificationStatus;
import com.farmeazy.entity.User;
import com.farmeazy.entity.UserBankDetails;
import com.farmeazy.repository.BankVerificationRequestRepository;
import com.farmeazy.repository.UserBankDetailsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingEligibilityServiceTest {

    @Mock
    private BankVerificationRequestRepository bankVerificationRequestRepository;

    @Mock
    private UserBankDetailsRepository userBankDetailsRepository;

    @InjectMocks
    private ListingEligibilityService listingEligibilityService;

    @Test
    void dashboardAndListingsRemainLockedUntilBankVerificationIsCompleted() {
        User user = buildEligibleVendor();
        UserBankDetails bankDetails = buildBankDetails(user);

        when(userBankDetailsRepository.findByUserId(1L)).thenReturn(Optional.of(bankDetails));
        when(bankVerificationRequestRepository.existsByUserIdAndStatus(1L, VerificationStatus.VERIFIED)).thenReturn(false);
        when(bankVerificationRequestRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        Map<String, Object> eligibility = listingEligibilityService.getEligibility(user, "PRODUCT");

        assertFalse(Boolean.TRUE.equals(eligibility.get("vendorDashboardEligible")));
        assertFalse(Boolean.TRUE.equals(eligibility.get("eligible")));
        assertFalse(Boolean.TRUE.equals(eligibility.get("canSellProducts")));
        assertFalse(Boolean.TRUE.equals(eligibility.get("canSellServices")));
        assertTrue(((java.util.List<?>) eligibility.get("missingRequirements")).stream()
                .map(String::valueOf)
                .anyMatch(item -> item.toLowerCase().contains("bank verification")));
    }

    @Test
    void dashboardAndListingsUnlockAfterBankVerification() {
        User user = buildEligibleVendor();
        UserBankDetails bankDetails = buildBankDetails(user);

        when(userBankDetailsRepository.findByUserId(1L)).thenReturn(Optional.of(bankDetails));
        when(bankVerificationRequestRepository.existsByUserIdAndStatus(1L, VerificationStatus.VERIFIED)).thenReturn(true);
        when(bankVerificationRequestRepository.findTopByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

        Map<String, Object> eligibility = listingEligibilityService.getEligibility(user, "PRODUCT");

        assertTrue(Boolean.TRUE.equals(eligibility.get("vendorDashboardEligible")));
        assertTrue(Boolean.TRUE.equals(eligibility.get("eligible")));
        assertTrue(Boolean.TRUE.equals(eligibility.get("canSellProducts")));
        assertTrue(Boolean.TRUE.equals(eligibility.get("canSellServices")));
        assertFalse(((java.util.List<?>) eligibility.get("missingRequirements")).stream()
                .map(String::valueOf)
                .anyMatch(item -> item.toLowerCase().contains("bank verification")));
    }

    private User buildEligibleVendor() {
        User user = new User();
        user.setId(1L);
        user.setEmail("vendor@example.com");
        user.setUsername("vendor");
        user.setPassword("secret");
        user.setPhone("9876543210");
        user.setAddress("H.No 10, Market Street");
        user.setCity("Pune");
        user.setState("Maharashtra");
        user.setActive(true);
        return user;
    }

    private UserBankDetails buildBankDetails(User user) {
        UserBankDetails bankDetails = new UserBankDetails();
        bankDetails.setUser(user);
        bankDetails.setAccountHolderName("Vendor Name");
        bankDetails.setAccountNumber("123456789012");
        bankDetails.setIfscCode("IFSC0001");
        bankDetails.setBankName("Farm Bank");
        return bankDetails;
    }
}
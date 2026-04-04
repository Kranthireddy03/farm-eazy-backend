package com.farmeazy.service;

import com.farmeazy.entity.BankVerificationRequest.VerificationStatus;
import com.farmeazy.entity.BankVerificationRequest;
import com.farmeazy.entity.UserBankDetails;
import com.farmeazy.entity.User;
import com.farmeazy.repository.BankVerificationRequestRepository;
import com.farmeazy.repository.UserBankDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ListingEligibilityService {

    @Autowired
    private BankVerificationRequestRepository bankVerificationRequestRepository;

    @Autowired
    private UserBankDetailsRepository userBankDetailsRepository;

    public Map<String, Object> getEligibility(User user, String listingType) {
        Map<String, Object> result = new HashMap<>();
        List<String> missingRequirements = new ArrayList<>();

        boolean accountActive = user != null && Boolean.TRUE.equals(user.getActive());
        boolean hasPhone = user != null && user.getPhone() != null && !user.getPhone().isBlank();
        boolean hasAddress = user != null
                && user.getCity() != null && !user.getCity().isBlank()
                && user.getState() != null && !user.getState().isBlank();

        Optional<UserBankDetails> userBankDetails = user != null
            ? userBankDetailsRepository.findByUserId(user.getId())
            : Optional.empty();
        boolean vendorDetailsCompleted = userBankDetails
            .map(this::hasRequiredVendorDetails)
            .orElse(false);

        boolean vendorDashboardEligible = accountActive && hasPhone && hasAddress;

        boolean bankVerified = user != null
                && bankVerificationRequestRepository.existsByUserIdAndStatus(user.getId(), VerificationStatus.VERIFIED);

        BankVerificationRequest latestVerification = user == null
            ? null
            : bankVerificationRequestRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        boolean verificationInProgress = isVerificationInProgress(latestVerification);

        if (!accountActive) {
            missingRequirements.add("Account must be active");
        }
        if (!hasPhone) {
            missingRequirements.add("Phone number is required");
        }
        if (!hasAddress) {
            missingRequirements.add("Profile location (city and state) is required");
        }
        if (!vendorDetailsCompleted) {
            missingRequirements.add("Complete vendor verification (bank details) before listing");
        }
        if (!bankVerified) {
            missingRequirements.add("Bank verification is required for paid listings");
        }

        boolean eligible = missingRequirements.isEmpty();

        result.put("listingType", listingType == null ? "PRODUCT" : listingType);
        result.put("accountActive", accountActive);
        result.put("profilePhoneReady", hasPhone);
        result.put("profileLocationReady", hasAddress);
        result.put("vendorDetailsCompleted", vendorDetailsCompleted);
        result.put("vendorDashboardEligible", vendorDashboardEligible);
        result.put("bankVerified", bankVerified);
        result.put("canSellProducts", eligible);
        result.put("canSellServices", eligible);
        result.put("eligible", eligible);
        result.put("vendorDashboardLocked", !vendorDashboardEligible);
        result.put("verificationInProgress", verificationInProgress);
        result.put("latestVerificationStatus", latestVerification != null && latestVerification.getStatus() != null
            ? latestVerification.getStatus().name()
            : null);
        result.put("verificationMessage", buildVerificationMessage(vendorDashboardEligible, verificationInProgress));
        result.put("verificationRedirectPath", "/vendor-verification");
        result.put("missingRequirements", missingRequirements);

        return result;
    }

    public void assertEligible(User user, String listingType) {
        Map<String, Object> eligibility = getEligibility(user, listingType);
        Boolean eligible = (Boolean) eligibility.get("eligible");
        if (!Boolean.TRUE.equals(eligible)) {
            @SuppressWarnings("unchecked")
            List<String> missing = (List<String>) eligibility.get("missingRequirements");
            String reason = missing == null || missing.isEmpty()
                    ? "Listing eligibility requirements are not met"
                    : String.join(". ", missing);
            throw new IllegalArgumentException("Cannot create " + (listingType == null ? "listing" : listingType.toLowerCase()) + ": " + reason);
        }
    }

    private boolean hasRequiredVendorDetails(UserBankDetails details) {
        return details.getAccountHolderName() != null && !details.getAccountHolderName().isBlank()
                && details.getAccountNumber() != null && !details.getAccountNumber().isBlank()
                && details.getIfscCode() != null && !details.getIfscCode().isBlank()
                && details.getBankName() != null && !details.getBankName().isBlank();
    }

    private boolean isVerificationInProgress(BankVerificationRequest request) {
        if (request == null || request.getStatus() == null) {
            return false;
        }
        VerificationStatus status = request.getStatus();
        return status == VerificationStatus.INITIATED
                || status == VerificationStatus.TRANSFER_PENDING
                || status == VerificationStatus.TRANSFER_SUCCESS;
    }

    private String buildVerificationMessage(boolean eligible, boolean verificationInProgress) {
        if (eligible) {
            return "Vendor verification complete. You can access vendor dashboard and list products/services.";
        }
        if (verificationInProgress) {
            return "Vendor verification is in progress. After successful verification, vendor dashboard and paid listings will be unlocked.";
        }
        return "To access vendor dashboard and list paid products/services, complete vendor verification (bank details) first.";
    }
}

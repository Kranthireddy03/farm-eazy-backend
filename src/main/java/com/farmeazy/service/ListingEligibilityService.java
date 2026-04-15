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
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ListingEligibilityService {

    private static final Pattern PHONE_10_DIGIT = Pattern.compile("^[0-9]{10}$");

    @Autowired
    private BankVerificationRequestRepository bankVerificationRequestRepository;

    @Autowired
    private UserBankDetailsRepository userBankDetailsRepository;

    public Map<String, Object> getEligibility(User user, String listingType) {
        Map<String, Object> result = new HashMap<>();
        List<String> missingRequirements = new ArrayList<>();

        String normalizedListingType = normalizeListingType(listingType);
        boolean accountActive = user != null && Boolean.TRUE.equals(user.getActive());
        boolean hasEmail = user != null && user.getEmail() != null && !user.getEmail().isBlank();
        boolean hasPhone = user != null
            && user.getPhone() != null
            && PHONE_10_DIGIT.matcher(user.getPhone().trim()).matches();
        boolean hasAddressLine = user != null && user.getAddress() != null && !user.getAddress().isBlank();
        boolean hasAddress = user != null
                && user.getCity() != null && !user.getCity().isBlank()
                && user.getState() != null && !user.getState().isBlank();

        Optional<UserBankDetails> userBankDetails = user != null
            ? userBankDetailsRepository.findByUserId(user.getId())
            : Optional.empty();
        boolean vendorDetailsCompleted = userBankDetails
            .map(this::hasRequiredVendorDetails)
            .orElse(false);

        boolean bankVerificationRequired = true;
        boolean bankVerified = user != null
                && bankVerificationRequestRepository.existsByUserIdAndStatus(user.getId(), VerificationStatus.VERIFIED);
        boolean vendorDashboardEligible = accountActive
                && hasEmail
                && hasPhone
                && hasAddressLine
                && hasAddress
                && vendorDetailsCompleted
                && bankVerified;

        boolean hasTransferSuccessAwaitingManualConfirmation = user != null
                && bankVerificationRequestRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .map(request -> request.getStatus() == VerificationStatus.TRANSFER_SUCCESS)
                .orElse(false);

        BankVerificationRequest latestVerification = user == null
            ? null
            : bankVerificationRequestRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        boolean verificationInProgress = isVerificationInProgress(latestVerification);

        if (!accountActive) {
            missingRequirements.add("Account must be active");
        }
        if (!hasEmail) {
            missingRequirements.add("Email is required");
        }
        if (!hasPhone) {
            missingRequirements.add("Phone number is required (10 digits)");
        }
        if (!hasAddressLine) {
            missingRequirements.add("Address is required");
        }
        if (!hasAddress) {
            missingRequirements.add("Profile location (city and state) is required");
        }
        if (!vendorDetailsCompleted) {
            missingRequirements.add("Complete vendor verification (bank details) before listing");
        }
        if (!bankVerified) {
            if (hasTransferSuccessAwaitingManualConfirmation) {
                missingRequirements.add("Manual penny drop confirmation is pending (confirm INR 1 receipt)");
            } else {
                missingRequirements.add("Bank verification with manual penny drop confirmation is required");
            }
        }

        boolean eligible = vendorDashboardEligible;
        String verificationRedirectPath = determineVerificationRedirectPath(hasEmail, hasPhone, hasAddressLine, hasAddress, vendorDetailsCompleted, bankVerified);

        result.put("listingType", normalizedListingType);
        result.put("accountActive", accountActive);
        result.put("profileEmailReady", hasEmail);
        result.put("profilePhoneReady", hasPhone);
        result.put("profileAddressReady", hasAddressLine);
        result.put("profileLocationReady", hasAddress);
        result.put("vendorDetailsCompleted", vendorDetailsCompleted);
        result.put("vendorDashboardEligible", vendorDashboardEligible);
        result.put("bankVerified", bankVerified);
        result.put("bankVerificationRequired", bankVerificationRequired);
        result.put("canSellProducts", eligible);
        result.put("canSellServices", eligible);
        result.put("eligible", eligible);
        result.put("vendorDashboardLocked", !vendorDashboardEligible);
        result.put("verificationInProgress", verificationInProgress);
        result.put("latestVerificationStatus", latestVerification != null && latestVerification.getStatus() != null
            ? latestVerification.getStatus().name()
            : null);
        result.put("verificationMessage", buildVerificationMessage(vendorDashboardEligible, verificationInProgress, bankVerificationRequired, bankVerified));
        result.put("verificationRedirectPath", verificationRedirectPath);
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

    private String buildVerificationMessage(boolean eligible, boolean verificationInProgress, boolean bankVerificationRequired, boolean bankVerified) {
        if (eligible) {
            return "Vendor verification complete. You can access vendor dashboard and list products/services.";
        }
        if (verificationInProgress) {
            return "Vendor verification is in progress. After successful manual penny-drop confirmation, vendor dashboard and listings will be unlocked.";
        }
        if (bankVerificationRequired && !bankVerified) {
            return "To access vendor dashboard and list products/services, complete vendor onboarding (email, phone, address, bank verification and manual penny-drop confirmation).";
        }
        return "To access vendor dashboard and list products/services, complete vendor onboarding requirements first.";
    }

    private String determineVerificationRedirectPath(
            boolean hasEmail,
            boolean hasPhone,
            boolean hasAddressLine,
            boolean hasAddress,
            boolean vendorDetailsCompleted,
            boolean bankVerified) {
        if (!hasEmail || !hasPhone || !hasAddressLine || !hasAddress) {
            return "/vendor-onboarding";
        }
        if (!vendorDetailsCompleted || !bankVerified) {
            return "/vendor-verification";
        }
        return "/vendor-dashboard";
    }

    private String normalizeListingType(String listingType) {
        if (listingType == null || listingType.isBlank()) {
            return "PRODUCT";
        }
        return listingType.trim().toUpperCase(Locale.ROOT);
    }

    private boolean requiresBankVerification(String listingType) {
        return true;
    }
}

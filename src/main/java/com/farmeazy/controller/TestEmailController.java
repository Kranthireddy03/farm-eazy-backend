package com.farmeazy.controller;

import com.farmeazy.dto.SmsResponseDto;
import com.farmeazy.service.EmailService;
import com.farmeazy.service.HttpEmailService;
import com.farmeazy.service.SmsService;
import com.farmeazy.service.UnifiedEmailService;
import com.farmeazy.sms.SmsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test-email")
public class TestEmailController {
    private static final String DEFAULT_TO = "kranthijambuluri@gmail.com";
    private static final String DEFAULT_NAME = "Kranthi J";
    private static final String DEFAULT_PHONE = "6301630368";

    @GetMapping("/zoho")
    public String sendZohoTestEmailGet(@RequestParam String to, @RequestParam String name) {
        boolean result = unifiedEmailService.sendWelcomeEmail(to, name);
        return result ? "Zoho email sent successfully to " + to : "Zoho email failed for " + to;
    }

    @Autowired
    private UnifiedEmailService unifiedEmailService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private HttpEmailService httpEmailService;

    @Autowired
    private SmsService smsService;

    @PostMapping("/zoho")
    public String sendZohoTestEmail(@RequestParam String to, @RequestParam String name) {
        boolean result = unifiedEmailService.sendWelcomeEmail(to, name);
        return result ? "Zoho email sent successfully to " + to : "Zoho email failed for " + to;
    }

    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> triggerAllEmailTemplates(
            @RequestParam(required = false, defaultValue = DEFAULT_TO) String to,
            @RequestParam(required = false, defaultValue = DEFAULT_NAME) String name) {

        Map<String, Boolean> results = new LinkedHashMap<>();

        runBooleanTest(results, "unified.welcome", () -> unifiedEmailService.sendWelcomeEmail(to, name));
        runBooleanTest(results, "unified.passwordReset", () -> unifiedEmailService.sendPasswordResetEmail(to, "654321"));
        runBooleanTest(results, "unified.otp", () -> unifiedEmailService.sendOtpEmail(to, name, "123456", "LOGIN"));
        runBooleanTest(results, "unified.notification", () -> unifiedEmailService.sendNotification(to, name, "Test Notification", "This is a complete email template test."));
        runBooleanTest(results, "unified.orderConfirmation", () -> unifiedEmailService.sendOrderConfirmationEmail(to, name, 10001L, "1200.00", "50", "18.00", "1168.00"));

        runVoidTest(results, "emailService.irrigationReminder", () -> emailService.sendIrrigationReminder(to, name, "Demo Farm", "Tomato", "15 Apr 2026 09:00 AM"));
        runVoidTest(results, "emailService.harvestNotification", () -> emailService.sendHarvestNotification(to, name, "Demo Farm", "Rice", "30 Apr 2026"));
        runVoidTest(results, "emailService.bankVerificationStatus", () -> emailService.sendBankVerificationStatusEmail(
            to,
            name,
            "BV-1001",
            "2500.00",
            "REF-9988",
            "XXXXXX1234",
            true,
            null,
            name,
            "HDFC Bank",
            "HDFC0000123",
            java.time.LocalDateTime.now(),
            "SYSTEM",
            "VERIFIED"
        ));
        runVoidTest(results, "emailService.serviceRequestConfirmation", () -> emailService.sendServiceRequestConfirmationEmail(
            to,
            name,
            "SR-1001",
            "Soil testing request",
            java.time.LocalDateTime.now(),
            "Tomorrow 10:00 AM",
            "OPEN",
            "Guntur",
            "Need full soil quality report"
        ));
        runVoidTest(results, "emailService.serviceRequestSupportAlert", () -> emailService.sendServiceRequestSupportAlertEmail(
            to,
            "SR-1001",
            "SOIL_TESTING",
            "HIGH",
            "Soil testing request",
            name,
            to,
            DEFAULT_PHONE,
            "Need urgent soil testing before sowing",
            "ORD-1001",
            java.time.LocalDateTime.now()
        ));
        runVoidTest(results, "emailService.serviceRequestStatusUpdate", () -> emailService.sendServiceRequestStatusUpdateEmail(
            to,
            name,
            "SR-1001",
            "OPEN",
            "IN_PROGRESS",
            "Your request is being processed by support."
        ));

        runBooleanTest(results, "http.coinEarned", () -> httpEmailService.sendCoinEarnedNotification(to, name, 25, 275, "Demo reward"));
        runBooleanTest(results, "http.coinSpent", () -> httpEmailService.sendCoinSpentNotification(to, name, 10, 265));
        runBooleanTest(results, "http.productUpdated", () -> httpEmailService.sendProductUpdateConfirmation(to, name, "Organic Tomato", "Vegetables", 45.0, 20, "kg"));
        runBooleanTest(results, "http.productDeleted", () -> httpEmailService.sendProductDeleteConfirmation(to, name, "Old Onion", "Vegetables", 25.0, 5, "kg"));
        runBooleanTest(results, "http.refundRequested", () -> httpEmailService.sendRefundRequestedNotification(to, name, 10002L, "PARTIAL", "Damaged package"));
        runBooleanTest(results, "http.refundSuccess", () -> httpEmailService.sendRefundSuccessNotification(to, name, 10002L, 15L, new BigDecimal("150.00"), "RFND-1002", "PARTIAL"));
        runBooleanTest(results, "http.refundFailed", () -> httpEmailService.sendRefundFailedNotification(to, name, 10002L, "Payment gateway timeout"));
        runBooleanTest(results, "http.orderCancellation", () -> httpEmailService.sendOrderCancellationNotification(to, name, 10003L, "Customer changed mind", new BigDecimal("89.00"), 5L, true));
        runBooleanTest(results, "http.returnRequest", () -> httpEmailService.sendReturnRequestNotification(to, name, 10004L, "Received wrong item", new BigDecimal("249.00"), 0L));
        runBooleanTest(results, "http.serviceListingCreated", () -> httpEmailService.sendServiceListingCreatedNotification(to, name, "Tractor Rental", 1500.0, "Per acre service"));
        runBooleanTest(results, "http.serviceListingUpdated", () -> httpEmailService.sendServiceListingUpdatedNotification(to, name, "Tractor Rental", 1750.0, "Updated rate"));
        runBooleanTest(results, "http.serviceListingDeleted", () -> httpEmailService.sendServiceListingDeletedNotification(to, name, "Legacy Service", 1000.0, "Removed by owner"));
        runBooleanTest(results, "http.serviceBookingApproved", () -> httpEmailService.sendServiceBookingApprovedNotification(to, name, "Land Ploughing", "Guntur", "Farmer Services Team"));
        runBooleanTest(results, "http.serviceBookingDeclined", () -> httpEmailService.sendServiceBookingDeclinedNotification(to, name, "Harvesting", "Vijayawada", "Farmer Services Team"));

        long success = results.values().stream().filter(Boolean.TRUE::equals).count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("recipient", to);
        response.put("total", results.size());
        response.put("success", success);
        response.put("failed", results.size() - success);
        response.put("results", results);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sms/all")
    public ResponseEntity<Map<String, Object>> triggerAllSmsTemplates(
            @RequestParam(required = false, defaultValue = DEFAULT_PHONE) String phone,
            @RequestParam(required = false, defaultValue = DEFAULT_NAME) String userName) {

        Map<String, Object> results = new LinkedHashMap<>();

        for (SmsTemplate template : SmsTemplate.values()) {
            Map<String, String> variables = new LinkedHashMap<>();
            variables.put("user", userName);
            variables.put("otp", "123456");
            variables.put("time", "10");
            variables.put("rupees", "199");
            variables.put("orderID", "FZ1001");
            variables.put("BookingID", "BK1001");
            variables.put("serviceId", "SV1001");
            variables.put("serviceID", "SV1001");
            variables.put("IrrigationId", "IR1001");
            variables.put("farm", "DemoFarm");
            variables.put("farmName", "DemoFarm");
            variables.put("value", "updated");

            SmsResponseDto smsResponse = smsService.sendTemplateTest(phone, template, variables);

            Map<String, Object> status = new LinkedHashMap<>();
            status.put("success", smsResponse.isSuccess());
            status.put("message", smsResponse.getMessage());
            status.put("displayMessage", smsResponse.getDisplayMessage());
            results.put(template.name(), status);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("phone", phone);
        response.put("templateCount", SmsTemplate.values().length);
        response.put("results", results);
        return ResponseEntity.ok(response);
    }

    private void runBooleanTest(Map<String, Boolean> results, String key, java.util.function.Supplier<Boolean> action) {
        try {
            results.put(key, Boolean.TRUE.equals(action.get()));
        } catch (Exception ex) {
            results.put(key, false);
        }
    }

    private void runVoidTest(Map<String, Boolean> results, String key, Runnable action) {
        try {
            action.run();
            results.put(key, true);
        } catch (Exception ex) {
            results.put(key, false);
        }
    }
}

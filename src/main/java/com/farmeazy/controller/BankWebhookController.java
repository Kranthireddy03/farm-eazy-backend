package com.farmeazy.controller;

import com.farmeazy.service.BankVerificationService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/razorpay/webhook/bank-verification")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class BankWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(BankWebhookController.class);

    private final BankVerificationService bankVerificationService;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    public BankWebhookController(BankVerificationService bankVerificationService) {
        this.bankVerificationService = bankVerificationService;
    }

    @PostMapping
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (payload == null || payload.isBlank() || signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "failure", "message", "Invalid webhook request"));
        }

        if (!isValidWebhookSignature(payload, signature)) {
            logger.warn("BANK_VERIFY_WEBHOOK_SIGNATURE_INVALID");
            return ResponseEntity.status(401).body(Map.of("status", "failure", "message", "Invalid signature"));
        }

        try {
            JSONObject json = new JSONObject(payload);
            String event = json.optString("event", "");
            boolean isTransferEvent = event.startsWith("transfer.");
            boolean isPayoutEvent = event.startsWith("payout.");

            if (!isTransferEvent && !isPayoutEvent) {
                return ResponseEntity.ok(Map.of("status", "ignored"));
            }

            JSONObject eventEntity = json
                    .optJSONObject("payload")
                    .optJSONObject(isPayoutEvent ? "payout" : "transfer")
                    .optJSONObject("entity");

            if (eventEntity == null) {
                return ResponseEntity.ok(Map.of("status", "ignored", "message", "Event entity missing"));
            }

            String referenceId = eventEntity.optString("id", "");
            String failureReason = extractFailureReason(eventEntity);

            if (referenceId.isBlank()) {
                return ResponseEntity.ok(Map.of("status", "ignored", "message", "Reference id missing"));
            }

            if ("transfer.processed".equals(event) || "payout.processed".equals(event)) {
                bankVerificationService.handleTransferProcessedWebhook(referenceId);
            } else if ("transfer.failed".equals(event) || "payout.failed".equals(event) || "payout.reversed".equals(event)) {
                bankVerificationService.handleTransferFailedWebhook(referenceId, failureReason);
            }

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception ex) {
            logger.error("BANK_VERIFY_WEBHOOK_PROCESSING_FAILED", ex);
            return ResponseEntity.status(500).body(Map.of("status", "failure", "message", "Webhook processing failed"));
        }
    }

    private boolean isValidWebhookSignature(String payload, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hmac = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmac) {
                sb.append(String.format("%02x", b));
            }
            return signature.equals(sb.toString());
        } catch (Exception ex) {
            logger.error("BANK_VERIFY_WEBHOOK_SIGNATURE_CHECK_FAILED", ex);
            return false;
        }
    }

    private String extractFailureReason(JSONObject entity) {
        JSONObject statusDetails = entity.optJSONObject("status_details");
        if (statusDetails != null) {
            String description = statusDetails.optString("description", "");
            if (!description.isBlank()) {
                return description;
            }
            String reason = statusDetails.optString("reason", "");
            if (!reason.isBlank()) {
                return reason;
            }
        }
        String status = entity.optString("status", "failed");
        return "Transfer status: " + status;
    }
}

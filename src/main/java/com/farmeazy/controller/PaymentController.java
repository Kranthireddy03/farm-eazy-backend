package com.farmeazy.controller;

import com.farmeazy.service.SmsService;
import com.farmeazy.service.HttpEmailService;
import com.razorpay.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    
    @Autowired
    private SmsService smsService;
    
    @Autowired
    private HttpEmailService emailService;
    
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody String payload, @RequestHeader Map<String, String> headers) {
        // TODO: Validate webhook signature using Razorpay's X-Razorpay-Signature header
        // Parse payload, update payment status in DB, trigger email/SMS if needed
        return ResponseEntity.ok("Webhook received");
    }

    @Value("${razorpay.key.id}")
    private String keyId;
    @Value("${razorpay.key.secret}")
    private String keySecret;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) {
        System.out.println("/create-order request: " + data);
        System.out.println("Razorpay keyId: " + keyId);
        System.out.println("Razorpay keySecret: " + (keySecret != null ? "[set]" : "[null]"));
        try {
            System.out.println("Before RazorpayClient init");
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            System.out.println("After RazorpayClient init");
            if (data == null || !data.containsKey("amount") || ((Number)data.get("amount")).intValue() <= 0) {
                System.out.println("Invalid or missing amount");
                return ResponseEntity.badRequest().body("Invalid or missing amount");
            }
            int amountInPaise = ((Number)data.get("amount")).intValue();
            String email = (String) data.get("email");
            String phone = (String) data.get("phone");
            System.out.println("Order params: amount=" + amountInPaise + ", email=" + email + ", phone=" + phone);

            if (amountInPaise < 100) {
                throw new RuntimeException("Minimum order amount is ₹1");
            }

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise); // amount already in paise from frontend
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_rcptid_" + System.currentTimeMillis());
            orderRequest.put("payment_capture", 1);
            System.out.println("Order request JSON: " + orderRequest);

            Order order = client.orders.create(orderRequest);
            System.out.println("Order created: " + order);
            // Optionally save order info to DB here
            JSONObject response = new JSONObject(order.toString());
            response.put("email", email);
            response.put("phone", phone);
            response.put("key_id", keyId); // Add public Razorpay key for frontend
            return ResponseEntity.ok(response.toMap());
        } catch (Exception e) {
            System.out.println("Exception in /create-order: " + e);
            e.printStackTrace();
            return ResponseEntity.status(500).body("Order Failed: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> data) {
        try {
            String orderId = (String) data.get("orderId");
            String paymentId = (String) data.get("paymentId");
            String signature = (String) data.get("signature");
            String email = (String) data.get("email");
            String phone = (String) data.get("phone");

            // Fetch payment status from Razorpay API for logging only (optional)
            String razorpayStatus = null;
            try {
                RazorpayClient client = new RazorpayClient(keyId, keySecret);
                Payment payment = client.payments.fetch(paymentId);
                razorpayStatus = payment.get("status");
            } catch (Exception ex) {
                System.out.println("[Payment Verify] Could not fetch payment status from Razorpay: " + ex.getMessage());
            }

            // Signature verification (orderId|paymentId, UTF-8, hex)
            String payload = orderId + "|" + paymentId;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(keySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hmac = sha256_HMAC.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmac) {
                sb.append(String.format("%02x", b));
            }
            String expectedSignature = sb.toString();
            boolean verified = signature != null && signature.equals(expectedSignature);

            boolean isSuccess = verified;
            String status = isSuccess ? "success" : "failure";
            System.out.println("[Payment Verify] orderId=" + orderId + ", paymentId=" + paymentId + ", status=" + status + ", razorpayStatus=" + razorpayStatus + ", signatureVerified=" + verified);

            // Persist payment status in DB (pseudo-code)
            // Payment payment = paymentRepository.findByOrderId(orderId);
            // if (payment != null) {
            //     payment.setStatus(status.toUpperCase());
            //     payment.setTransactionId(paymentId);
            //     paymentRepository.save(payment);
            // }

            // Trigger email/SMS notification for payment success
            // NOTE: SMS is sent from OrderService when order is created with FarmEazy order ID
            // PaymentController only verifies payment, not creates order
            if (isSuccess) {
                return ResponseEntity.ok(Map.of("status", "success"));
            } else {
                return ResponseEntity.ok(Map.of("status", "failure"));
            }
        } catch (Exception e) {
            System.out.println("[Payment Verify] Exception: " + e.getMessage());
            return ResponseEntity.ok(Map.of("status", "failure"));
        }
    }
}

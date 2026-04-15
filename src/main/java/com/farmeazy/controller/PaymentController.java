package com.farmeazy.controller;

import com.farmeazy.entity.Order;
import com.farmeazy.entity.ServiceBooking;
import com.farmeazy.repository.OrderRepository;
import com.farmeazy.repository.ServiceBookingRepository;
import com.farmeazy.service.SmsService;
import com.farmeazy.service.HttpEmailService;
import com.razorpay.Payment;
import com.razorpay.RazorpayException;
import com.razorpay.RazorpayClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private SmsService smsService;
    
    @Autowired
    private HttpEmailService emailService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ServiceBookingRepository serviceBookingRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;
    
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        logger.info("PAYMENT_CONTROLLER_WEBHOOK_RECEIVED payloadSize={} signaturePresent={}",
                payload != null ? payload.length() : 0,
                signature != null && !signature.isBlank());

        if (payload == null || payload.isBlank() || signature == null || signature.isBlank()) {
            logger.warn("PAYMENT_CONTROLLER_WEBHOOK_INVALID_REQUEST missingPayloadOrSignature");
            return ResponseEntity.badRequest().body(Map.of("status", "failure", "message", "Invalid webhook request"));
        }

        if (!isValidWebhookSignature(payload, signature)) {
            logger.warn("PAYMENT_CONTROLLER_WEBHOOK_SIGNATURE_INVALID");
            return ResponseEntity.status(401).body(Map.of("status", "failure", "message", "Invalid webhook signature"));
        }

        try {
            JSONObject json = new JSONObject(payload);
            String event = json.optString("event", "");
            JSONObject paymentEntity = json
                    .optJSONObject("payload")
                    .optJSONObject("payment")
                    .optJSONObject("entity");

            if (event.isBlank() || paymentEntity == null) {
                logger.warn("PAYMENT_CONTROLLER_WEBHOOK_UNSUPPORTED_PAYLOAD event={}", event);
                return ResponseEntity.ok(Map.of("status", "ignored", "message", "Unsupported payload"));
            }

            String razorpayOrderId = paymentEntity.optString("order_id", null);
            String razorpayPaymentId = paymentEntity.optString("id", null);
            String paymentStatus = paymentEntity.optString("status", "");

            if (razorpayOrderId == null || razorpayOrderId.isBlank() || razorpayPaymentId == null || razorpayPaymentId.isBlank()) {
                logger.warn("PAYMENT_CONTROLLER_WEBHOOK_MISSING_IDS event={} orderIdPresent={} paymentIdPresent={}",
                        event, razorpayOrderId != null && !razorpayOrderId.isBlank(), razorpayPaymentId != null && !razorpayPaymentId.isBlank());
                return ResponseEntity.ok(Map.of("status", "ignored", "message", "Payment IDs missing"));
            }

            if ("payment.captured".equals(event)) {
                processSuccessfulPayment(razorpayOrderId, razorpayPaymentId);
            } else if ("payment.failed".equals(event)) {
                processFailedPayment(razorpayOrderId, razorpayPaymentId, paymentStatus);
            } else {
                logger.info("PAYMENT_CONTROLLER_WEBHOOK_EVENT_IGNORED event={}", event);
            }

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception ex) {
            logger.error("PAYMENT_CONTROLLER_WEBHOOK_PROCESSING_FAILED", ex);
            return ResponseEntity.status(500).body(Map.of("status", "failure", "message", "Webhook processing failed"));
        }
    }

    @Value("${razorpay.key.id}")
    private String keyId;
    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${payment.simulation.enabled:false}")
    private boolean paymentSimulationEnabled;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) {
        logger.info("PAYMENT_CONTROLLER_CREATE_ORDER_REQUEST payloadKeys={}", data != null ? data.keySet() : null);
        logger.debug("PAYMENT_CONTROLLER_RAZORPAY_CONFIG keyIdPresent={} keySecretPresent={}", keyId != null && !keyId.isBlank(), keySecret != null && !keySecret.isBlank());
        try {
            if (data == null || !data.containsKey("amount") || ((Number)data.get("amount")).intValue() <= 0) {
                logger.warn("PAYMENT_CONTROLLER_CREATE_ORDER_INVALID_AMOUNT payload={}", data);
                return ResponseEntity.badRequest().body("Invalid or missing amount");
            }
            int amountInPaise = ((Number)data.get("amount")).intValue();
            String email = (String) data.get("email");
            String phone = (String) data.get("phone");
            logger.info("PAYMENT_CONTROLLER_CREATE_ORDER_PARAMS amountInPaise={} email={} phone={}", amountInPaise, email, phone);

            if (amountInPaise < 100) {
                throw new RuntimeException("Minimum order amount is ₹1");
            }

            String normalizedKeyId = keyId == null ? "" : keyId.trim();
            String normalizedKeySecret = keySecret == null ? "" : keySecret.trim();
            boolean missingKeySecret = normalizedKeySecret.isBlank();
            boolean simulationMode = paymentSimulationEnabled && missingKeySecret;
            if (simulationMode) {
                String simulatedOrderId = "order_sim_" + System.currentTimeMillis();
                String simulatedPaymentId = "pay_sim_" + System.currentTimeMillis();
                Map<String, Object> simulated = new LinkedHashMap<>();
                simulated.put("id", simulatedOrderId);
                simulated.put("amount", amountInPaise);
                simulated.put("currency", "INR");
                simulated.put("status", "created");
                simulated.put("key_id", keyId);
                simulated.put("email", email);
                simulated.put("phone", phone);
                simulated.put("simulation", true);
                simulated.put("simulation_payment_id", simulatedPaymentId);
                logger.warn("PAYMENT_CONTROLLER_CREATE_ORDER_SIMULATED orderId={} amountInPaise={} reason=missingKeySecret", simulatedOrderId, amountInPaise);
                return ResponseEntity.ok(simulated);
            }

            if (missingKeySecret) {
                logger.error("PAYMENT_CONTROLLER_CREATE_ORDER_BLOCKED reason=missingKeySecret simulationEnabled={}", paymentSimulationEnabled);
                return ResponseEntity.status(503).body(Map.of(
                        "status", "failure",
                        "message", "Razorpay is not configured. Please contact support."
                ));
            }

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise); // amount already in paise from frontend
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_rcptid_" + System.currentTimeMillis());
            orderRequest.put("payment_capture", 1);
            logger.debug("PAYMENT_CONTROLLER_CREATE_ORDER_REQUEST_BODY {}", orderRequest);

            com.razorpay.Order order = executeWithBreaker("paymentCreateOrder", () -> {
                try {
                    logger.debug("PAYMENT_CONTROLLER_RAZORPAY_CLIENT_INIT_START");
                    RazorpayClient client = new RazorpayClient(normalizedKeyId, normalizedKeySecret);
                    logger.debug("PAYMENT_CONTROLLER_RAZORPAY_CLIENT_INIT_DONE");
                    return client.orders.create(orderRequest);
                } catch (RazorpayException ex) {
                    throw new RuntimeException(ex);
                }
            });
            logger.info("PAYMENT_CONTROLLER_CREATE_ORDER_SUCCESS order={}", order);
            // Optionally save order info to DB here
            JSONObject response = new JSONObject(order.toString());
            response.put("email", email);
            response.put("phone", phone);
            response.put("key_id", normalizedKeyId); // Add public Razorpay key for frontend
            return ResponseEntity.ok(response.toMap());
        } catch (RuntimeException runtimeEx) {
            Throwable cause = runtimeEx.getCause();
            if (cause instanceof RazorpayException re) {
                logger.error("PAYMENT_CONTROLLER_CREATE_ORDER_RAZORPAY_ERROR message={}", re.getMessage(), re);
                return ResponseEntity.status(502).body(Map.of(
                        "status", "failure",
                        "message", "Razorpay authentication failed. Verify key id/secret pair in active profile."
                ));
            }
            logger.error("PAYMENT_CONTROLLER_CREATE_ORDER_RUNTIME_FAILURE", runtimeEx);
            return ResponseEntity.status(503).body(Map.of(
                    "status", "failure",
                    "message", "Payment service is temporarily unavailable. Please retry."
            ));
        } catch (Exception e) {
            logger.error("PAYMENT_CONTROLLER_CREATE_ORDER_FAILED", e);
            return ResponseEntity.status(500).body("Order Failed: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> data) {
        logger.info("PAYMENT_CONTROLLER_VERIFY_REQUEST payloadKeys={}", data != null ? data.keySet() : null);
        try {
            String orderId = (String) data.get("orderId");
            String paymentId = (String) data.get("paymentId");
            String signature = (String) data.get("signature");
            String email = (String) data.get("email");
            String phone = (String) data.get("phone");
            String normalizedKeySecret = keySecret == null ? "" : keySecret.trim();
            boolean missingKeySecret = normalizedKeySecret.isBlank();
            boolean simulationMode = (paymentSimulationEnabled && missingKeySecret) || Boolean.TRUE.equals(data.get("simulation"));

            if (simulationMode) {
                logger.info("PAYMENT_CONTROLLER_VERIFY_SIMULATED orderId={} paymentId={} email={} phone={}", orderId, paymentId, email, phone);
                return ResponseEntity.ok(Map.of("status", "success", "simulation", true));
            }

            if (missingKeySecret) {
                logger.error("PAYMENT_CONTROLLER_VERIFY_BLOCKED reason=missingKeySecret simulationEnabled={}", paymentSimulationEnabled);
                return ResponseEntity.status(503).body(Map.of(
                        "status", "failure",
                        "message", "Razorpay verification unavailable. Missing configuration."
                ));
            }

            // Fetch payment status from Razorpay API for logging only (optional)
            String razorpayStatus = null;
            try {
                razorpayStatus = executeWithBreaker("paymentVerifyFetch", () -> {
                    try {
                        RazorpayClient client = new RazorpayClient(keyId, keySecret);
                        Payment payment = client.payments.fetch(paymentId);
                        return payment.get("status");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
            } catch (Exception ex) {
                logger.warn("PAYMENT_CONTROLLER_VERIFY_FETCH_STATUS_FAILED paymentId={} message={}", paymentId, ex.getMessage());
            }

            // Signature verification (orderId|paymentId, UTF-8, hex)
            String payload = orderId + "|" + paymentId;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(normalizedKeySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
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
            logger.info("PAYMENT_CONTROLLER_VERIFY_RESULT orderId={} paymentId={} status={} razorpayStatus={} signatureVerified={} email={} phone={}", orderId, paymentId, status, razorpayStatus, verified, email, phone);

            if (isSuccess && razorpayStatus != null && ("captured".equalsIgnoreCase(razorpayStatus) || "authorized".equalsIgnoreCase(razorpayStatus))) {
                processSuccessfulPayment(orderId, paymentId);
            }

            // Trigger email/SMS notification for payment success
            // NOTE: SMS is sent from OrderService when order is created with FarmEazy order ID
            // PaymentController only verifies payment, not creates order
            if (isSuccess) {
                return ResponseEntity.ok(Map.of("status", "success"));
            } else {
                return ResponseEntity.ok(Map.of("status", "failure"));
            }
        } catch (Exception e) {
            logger.error("PAYMENT_CONTROLLER_VERIFY_EXCEPTION", e);
            return ResponseEntity.ok(Map.of("status", "failure"));
        }
    }

    private void processSuccessfulPayment(String razorpayOrderId, String razorpayPaymentId) {
        Optional<Order> optionalOrder = orderRepository.findByRazorpayOrderId(razorpayOrderId);
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            if (Order.PaymentStatus.COMPLETED.equals(order.getPaymentStatus())) {
                logger.info("PAYMENT_CONTROLLER_ORDER_ALREADY_COMPLETED orderId={} razorpayPaymentId={}", order.getId(), order.getRazorpayPaymentId());
                return;
            }
            if (Order.PaymentStatus.CANCELLED.equals(order.getPaymentStatus())) {
                logger.warn("PAYMENT_CONTROLLER_ORDER_PAYMENT_IGNORED_CANCELLED orderId={}", order.getId());
                return;
            }

            order.setRazorpayOrderId(razorpayOrderId);
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setTransactionId(razorpayPaymentId);
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
            order.setOrderStatus(Order.OrderStatus.CONFIRMED);
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);
            logger.info("PAYMENT_CONTROLLER_ORDER_PAYMENT_COMPLETED orderId={} razorpayPaymentId={}", order.getId(), razorpayPaymentId);
            return;
        }

        Optional<ServiceBooking> optionalBooking = serviceBookingRepository.findByRazorpayOrderId(razorpayOrderId);
        if (optionalBooking.isPresent()) {
            ServiceBooking booking = optionalBooking.get();
            if (ServiceBooking.PaymentStatus.SUCCESS.equals(booking.getPaymentStatus())) {
                logger.info("PAYMENT_CONTROLLER_BOOKING_ALREADY_COMPLETED bookingId={} razorpayPaymentId={}", booking.getId(), booking.getRazorpayPaymentId());
                return;
            }

            booking.setRazorpayOrderId(razorpayOrderId);
            booking.setRazorpayPaymentId(razorpayPaymentId);
            booking.setTransactionId(razorpayPaymentId);
            booking.setPaymentStatus(ServiceBooking.PaymentStatus.SUCCESS);
            booking.setStatus(ServiceBooking.BookingStatus.CONFIRMED);
            booking.setPaidAt(LocalDateTime.now());
            serviceBookingRepository.save(booking);
            logger.info("PAYMENT_CONTROLLER_BOOKING_PAYMENT_COMPLETED bookingId={} razorpayPaymentId={}", booking.getId(), razorpayPaymentId);
            return;
        }

        logger.warn("PAYMENT_CONTROLLER_PAYMENT_TARGET_NOT_FOUND razorpayOrderId={} razorpayPaymentId={}", razorpayOrderId, razorpayPaymentId);
    }

    private void processFailedPayment(String razorpayOrderId, String razorpayPaymentId, String gatewayStatus) {
        Optional<Order> optionalOrder = orderRepository.findByRazorpayOrderId(razorpayOrderId);
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            if (Order.PaymentStatus.COMPLETED.equals(order.getPaymentStatus())) {
                logger.warn("PAYMENT_CONTROLLER_ORDER_FAILURE_IGNORED_COMPLETED orderId={}", order.getId());
                return;
            }
            order.setRazorpayOrderId(razorpayOrderId);
            order.setRazorpayPaymentId(razorpayPaymentId);
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            orderRepository.save(order);
            logger.info("PAYMENT_CONTROLLER_ORDER_PAYMENT_FAILED orderId={} gatewayStatus={}", order.getId(), gatewayStatus);
            return;
        }

        Optional<ServiceBooking> optionalBooking = serviceBookingRepository.findByRazorpayOrderId(razorpayOrderId);
        if (optionalBooking.isPresent()) {
            ServiceBooking booking = optionalBooking.get();
            if (ServiceBooking.PaymentStatus.SUCCESS.equals(booking.getPaymentStatus())) {
                logger.warn("PAYMENT_CONTROLLER_BOOKING_FAILURE_IGNORED_COMPLETED bookingId={}", booking.getId());
                return;
            }
            booking.setRazorpayOrderId(razorpayOrderId);
            booking.setRazorpayPaymentId(razorpayPaymentId);
            booking.setPaymentStatus(ServiceBooking.PaymentStatus.FAILED);
            serviceBookingRepository.save(booking);
            logger.info("PAYMENT_CONTROLLER_BOOKING_PAYMENT_FAILED bookingId={} gatewayStatus={}", booking.getId(), gatewayStatus);
        }
    }

    private boolean isValidWebhookSignature(String payload, String receivedSignature) {
        try {
            if (webhookSecret == null || webhookSecret.isBlank()) {
                logger.error("PAYMENT_CONTROLLER_WEBHOOK_SECRET_MISSING");
                return false;
            }

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hmac = sha256Hmac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder signatureBuilder = new StringBuilder();
            for (byte b : hmac) {
                signatureBuilder.append(String.format("%02x", b));
            }
            String expectedSignature = signatureBuilder.toString();
            return expectedSignature.equals(receivedSignature);
        } catch (Exception e) {
            logger.error("PAYMENT_CONTROLLER_WEBHOOK_SIGNATURE_VERIFY_FAILED", e);
            return false;
        }
    }

    private <T> T executeWithBreaker(String breakerName, Supplier<T> action) {
        try {
            return circuitBreakerRegistry.circuitBreaker(breakerName).executeSupplier(action::get);
        } catch (CallNotPermittedException ex) {
            throw new RuntimeException("Circuit breaker is open for " + breakerName, ex);
        }
    }
}

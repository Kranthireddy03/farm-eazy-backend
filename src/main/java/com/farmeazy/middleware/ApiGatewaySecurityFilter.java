package com.farmeazy.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 12)
public class ApiGatewaySecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiGatewaySecurityFilter.class);

    @Value("${security.api.gateway.enabled:true}")
    private boolean gatewayEnabled;

    @Value("${security.api.gateway.required:true}")
    private boolean gatewayRequired;

    @Value("${security.api.gateway.shared-secret:}")
    private String sharedSecret;

    @Value("${security.api.gateway.allowed-clients:farmeazy-web,farmeazy-admin,farmeazy-frontend}")
    private String allowedClients;

    @Value("${security.api.gateway.max-skew-seconds:300}")
    private long maxSkewSeconds;

    @Value("${security.api.gateway.replay-protection.enabled:true}")
    private boolean replayProtectionEnabled;

    private final ConcurrentHashMap<String, Long> seenRequestSignatures = new ConcurrentHashMap<>();

    @Autowired
    private com.farmeazy.security.RedisNonceService redisNonceService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        if (!path.startsWith("/api/")) {
            return true;
        }
        if (path.startsWith("/api/auth/")
                || path.startsWith("/api/otp/")
            || path.startsWith("/api/test-email/")
                || path.startsWith("/api/public/")
                || path.startsWith("/api/faq-question")
                || path.startsWith("/api/faq-questions")
                || path.startsWith("/api/faq/question")) {
            return true;
        }
        if (path.startsWith("/api/admin/faq-questions/stream")) {
            return true;
        }
        return path.startsWith("/api/products/media/")
                || path.startsWith("/api/payment/webhook")
            || path.startsWith("/api/razorpay/webhook");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!gatewayEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = request.getHeader("X-Gateway-Client");
        String timestampHeader = request.getHeader("X-Gateway-Timestamp");
        String signature = request.getHeader("X-Gateway-Signature");
        String nonce = request.getHeader("X-Request-Nonce");

        // If gateway headers are not required, allow through
        if ((clientId == null || timestampHeader == null) && !gatewayRequired) {
            filterChain.doFilter(request, response);
            return;
        }

        // If headers are missing but the request already carries an Authorization bearer token,
        // allow it through. This permits browser-based JWT-authenticated requests that may not
        // include gateway headers (client-side builds or proxies that strip custom headers).
        if (clientId == null || timestampHeader == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            sendErrorWithCors(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Missing gateway security headers");
            return;
        }

        Set<String> clientSet = parseAllowedClients();
        if (!clientSet.contains(clientId)) {
            sendErrorWithCors(request, response, HttpServletResponse.SC_FORBIDDEN, "Unauthorized gateway client");
            return;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException ex) {
            sendErrorWithCors(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid gateway timestamp");
            return;
        }

        long nowMillis = Instant.now().toEpochMilli();
        long allowedSkewMillis = maxSkewSeconds * 1000;
        if (Math.abs(nowMillis - timestamp) > allowedSkewMillis) {
            sendErrorWithCors(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Gateway timestamp is outside the allowed window");
            return;
        }

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Two supported flows:
        // 1) Signature flow (machine clients): X-Gateway-Signature present -> validate HMAC using sharedSecret
        // 2) Nonce flow (browsers): no signature, require X-Request-Nonce for mutating requests and validate via Redis

        if (signature != null) {
            if (sharedSecret == null || sharedSecret.isBlank()) {
                logger.error("Gateway signature present but shared secret is not configured");
                sendErrorWithCors(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Gateway signature verification not configured");
                return;
            }

            String message = clientId + ":" + timestamp + ":" + method + ":" + path;
            String expectedSignature = hmacSha256Base64(message, sharedSecret);
            if (!constantTimeEquals(expectedSignature, signature)) {
                sendErrorWithCors(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid gateway signature");
                return;
            }

            if (replayProtectionEnabled && isMutatingMethod(method)) {
                long ttlMillis = allowedSkewMillis;
                // Prefer Redis for replay protection when available
                boolean recorded = true;
                try {
                    recorded = redisNonceService.recordNonce(clientId, signature, ttlMillis);
                } catch (Exception ex) {
                    // fallback to in-memory
                    cleanupExpiredReplayEntries(nowMillis - ttlMillis);
                    String replayKey = clientId + ":" + timestamp + ":" + method + ":" + path + ":" + signature;
                    Long existing = seenRequestSignatures.putIfAbsent(replayKey, nowMillis);
                    recorded = (existing == null);
                }
                if (!recorded) {
                    sendErrorWithCors(request, response, HttpServletResponse.SC_CONFLICT, "Replay request detected");
                    return;
                }
            }

        } else {
            // Nonce-based browser flow
            if (replayProtectionEnabled && isMutatingMethod(method)) {
                if (nonce == null || nonce.isBlank()) {
                    sendErrorWithCors(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Missing request nonce for mutating method");
                    return;
                }
                long ttlMillis = allowedSkewMillis;
                boolean recorded = redisNonceService.recordNonce(clientId, nonce, ttlMillis);
                if (!recorded) {
                    sendErrorWithCors(request, response, HttpServletResponse.SC_CONFLICT, "Replay request detected");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isMutatingMethod(String method) {
        if (method == null) {
            return false;
        }
        String normalized = method.trim().toUpperCase();
        return "POST".equals(normalized)
                || "PUT".equals(normalized)
                || "PATCH".equals(normalized)
                || "DELETE".equals(normalized);
    }

    private void cleanupExpiredReplayEntries(long expiryCutoffMillis) {
        Iterator<Map.Entry<String, Long>> iterator = seenRequestSignatures.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < expiryCutoffMillis) {
                iterator.remove();
            }
        }
    }

    private Set<String> parseAllowedClients() {
        Set<String> values = new HashSet<>();
        for (String raw : allowedClients.split(",")) {
            String value = raw.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private String hmacSha256Base64(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create gateway signature", ex);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private void sendErrorWithCors(HttpServletRequest request, HttpServletResponse response, int status, String message) throws IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Vary", "Origin");
        }
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}

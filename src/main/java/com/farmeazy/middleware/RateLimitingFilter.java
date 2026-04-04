package com.farmeazy.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RATE LIMITING FILTER - Limits requests per IP for sensitive auth endpoints.
 * Protects both user and admin portal authentication flows because they share backend auth APIs.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final long WINDOW_MILLIS = 60_000; // 1 minute rolling window
    private static final int CLEANUP_THRESHOLD = 10_000;
    private static final Map<String, Integer> ENDPOINT_LIMITS = new LinkedHashMap<>();

    static {
        ENDPOINT_LIMITS.put("/api/auth/login", 10);
        ENDPOINT_LIMITS.put("/api/auth/register", 6);
        ENDPOINT_LIMITS.put("/api/auth/refresh", 30);
        ENDPOINT_LIMITS.put("/api/auth/request-otp", 5);
        ENDPOINT_LIMITS.put("/api/auth/verify-otp", 20);
        ENDPOINT_LIMITS.put("/api/auth/login/request-otp", 5);
        ENDPOINT_LIMITS.put("/api/auth/login/verify-otp", 20);
        ENDPOINT_LIMITS.put("/api/auth/forgot-password", 5);
        ENDPOINT_LIMITS.put("/api/auth/register/availability", 30);
    }

    private final Map<String, RequestCounter> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        Integer maxAttempts = ENDPOINT_LIMITS.get(path);

        if (maxAttempts != null) {
            String ip = resolveClientIp(request);
            String limitKey = ip + "::" + path;
            long now = Instant.now().toEpochMilli();
            RequestCounter counter = attempts.computeIfAbsent(limitKey, k -> new RequestCounter());
            synchronized (counter) {
                if (now - counter.windowStart > WINDOW_MILLIS) {
                    counter.windowStart = now;
                    counter.count = 0;
                }
                counter.count++;
                if (counter.count > maxAttempts) {
                    long windowRemaining = WINDOW_MILLIS - (now - counter.windowStart);
                    long retryAfterSeconds = Math.max(1L, (windowRemaining + 999L) / 1000L);
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
                    response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please try again later.\",\"retryAfterSeconds\":" + retryAfterSeconds + "}");
                    return;
                }
            }

            // Keep in-memory map bounded in long-running processes.
            if (attempts.size() > CLEANUP_THRESHOLD) {
                long staleBefore = now - (WINDOW_MILLIS * 2);
                attempts.entrySet().removeIf(entry -> entry.getValue().windowStart < staleBefore);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestCounter {
        long windowStart = Instant.now().toEpochMilli();
        int count = 0;
    }
}

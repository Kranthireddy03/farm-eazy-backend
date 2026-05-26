package com.farmeazy.middleware;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RATE LIMITING FILTER - Limits requests per IP for sensitive auth endpoints.
 * Uses Redis so throttling is consistent across instances.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final long WINDOW_MILLIS = 60_000; // 1 minute rolling window
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

    private final StringRedisTemplate redisTemplate;
    // In-memory fallback counters when Redis is unavailable
    private static final ConcurrentHashMap<String, Integer> localCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> localExpiry = new ConcurrentHashMap<>();

    public RateLimitingFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        Integer maxAttempts = ENDPOINT_LIMITS.get(path);

        if (maxAttempts != null) {
            String ip = resolveClientIp(request);
            long bucket = Instant.now().toEpochMilli() / WINDOW_MILLIS;
            String key = "rate-limit:" + ip + ":" + path + ":" + bucket;

            try {
                Long currentCount = redisTemplate.opsForValue().increment(key);
                if (currentCount != null && currentCount == 1L) {
                    redisTemplate.expire(key, Duration.ofMillis(WINDOW_MILLIS * 2));
                }

                if (currentCount != null && currentCount > maxAttempts) {
                    long retryAfterSeconds = 60L;
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
                    response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please try again later.\",\"retryAfterSeconds\":" + retryAfterSeconds + "}");
                    return;
                }
            } catch (Exception exception) {
                // Redis is down/unreachable; use local in-memory fallback instead of failing the request
                logger.warn("Rate limiting Redis error for path={} ip={}: {}, falling back to in-memory counters", path, ip, exception.getMessage());

                // Clean up expired buckets if necessary and increment local counter
                long now = Instant.now().toEpochMilli();
                localExpiry.compute(key, (k, exp) -> {
                    if (exp == null || exp < now) return now + WINDOW_MILLIS * 2;
                    return exp;
                });

                Integer current = localCounts.merge(key, 1, Integer::sum);
                if (current != null && current > maxAttempts) {
                    long retryAfterSeconds = 60L;
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
                    response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please try again later.\",\"retryAfterSeconds\":" + retryAfterSeconds + "}");
                    return;
                }
                // allow the request to proceed under local counters
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
}

package com.farmeazy.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RATE LIMITING FILTER - Limits requests per IP for sensitive endpoints
 * Applies to /api/auth/login and /api/auth/register
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 60_000; // 1 minute
    private final Map<String, RequestCounter> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            String ip = request.getRemoteAddr();
            long now = Instant.now().toEpochMilli();
            RequestCounter counter = attempts.computeIfAbsent(ip, k -> new RequestCounter());
            synchronized (counter) {
                if (now - counter.windowStart > WINDOW_MILLIS) {
                    counter.windowStart = now;
                    counter.count = 0;
                }
                counter.count++;
                if (counter.count > MAX_ATTEMPTS) {
                    response.setStatus(429);
                    response.getWriter().write("Too many requests. Please try again later.");
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private static class RequestCounter {
        long windowStart = Instant.now().toEpochMilli();
        int count = 0;
    }
}

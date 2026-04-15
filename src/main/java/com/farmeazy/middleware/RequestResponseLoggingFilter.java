package com.farmeazy.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Value("${app.logging.trace-all:false}")
    private boolean traceAll;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/h2-console")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/favicon");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String endpoint = query == null ? path : path + "?" + query;
        String userAgent = request.getHeader("User-Agent");
        String requestId = Long.toHexString(System.nanoTime());
        if (traceAll || shouldTraceEndpoint(path)) {
            logger.info("HTTP_REQUEST id={} method={} endpoint={} remote={} userAgent={}",
                requestId, method, endpoint, request.getRemoteAddr(), userAgent);
        } else {
            logger.debug("HTTP_REQUEST id={} method={} endpoint={}", requestId, method, endpoint);
        }

        try {
            filterChain.doFilter(request, response);
            long duration = System.currentTimeMillis() - start;
                if (traceAll || shouldTraceEndpoint(path) || response.getStatus() >= 400) {
                logger.info("HTTP_RESPONSE id={} method={} endpoint={} status={} durationMs={}",
                    requestId, method, endpoint, response.getStatus(), duration);
                } else {
                logger.debug("HTTP_RESPONSE id={} method={} endpoint={} status={} durationMs={}",
                    requestId, method, endpoint, response.getStatus(), duration);
                }
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            logger.error("HTTP_ERROR id={} method={} endpoint={} durationMs={} message={}",
                    requestId, method, endpoint, duration, ex.getMessage());
            throw ex;
        }
    }

    private String valueOf(Object value) {
        if (value == null) {
            return "n/a";
        }
        String text = String.valueOf(value);
        if (text.length() > 512) {
            return text.substring(0, 512) + "...";
        }
        return text;
    }

    private boolean shouldTraceEndpoint(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/api/auth")
                || path.startsWith("/api/vendor")
                || path.startsWith("/api/payment")
                || path.startsWith("/api/support")
                || path.startsWith("/api/orders");
    }
}

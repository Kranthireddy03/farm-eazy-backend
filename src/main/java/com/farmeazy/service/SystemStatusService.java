package com.farmeazy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemStatusService {

    private static final Logger logger = LoggerFactory.getLogger(SystemStatusService.class);

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private final ConcurrentHashMap<String, String> lastFailureByService = new ConcurrentHashMap<>();

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    public SystemStatusService(JdbcTemplate jdbcTemplate,
                               RedisConnectionFactory redisConnectionFactory,
                               CircuitBreakerRegistry circuitBreakerRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public Map<String, Map<String, Object>> getFullStatus() {
        Map<String, Map<String, Object>> status = new LinkedHashMap<>();
        status.put("api", staticStatus("UP"));
        status.put("db", monitoredStatus("db", this::probeDatabase));
        status.put("redis", monitoredStatus("redis", this::probeRedis));
        status.put("payment", monitoredStatus("payment", this::probePayment));
        status.put("notification", monitoredStatus("notification", this::probeNotification));
        return status;
    }

    private Map<String, Object> staticStatus(String value) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", value);
        status.put("latencyMs", 0);
        status.put("circuitState", "CLOSED");
        status.put("lastFailure", null);
        status.put("checkedAt", Instant.now().toString());
        return status;
    }

    private Map<String, Object> monitoredStatus(String serviceName, Supplier<String> probe) {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(serviceName + "Probe");
        long startNanos = System.nanoTime();
        String statusValue;

        try {
            statusValue = breaker.executeSupplier(probe::get);
            lastFailureByService.remove(serviceName);
        } catch (CallNotPermittedException ex) {
            statusValue = "DOWN";
            lastFailureByService.put(serviceName, "Circuit is open");
        } catch (Exception ex) {
            statusValue = "DOWN";
            String message = ex.getMessage() == null ? "Unknown failure" : ex.getMessage();
            lastFailureByService.put(serviceName, message);
            logger.warn("SYSTEM_STATUS_{}_DOWN message={}", serviceName.toUpperCase(), message);
        }

        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", statusValue);
        status.put("latencyMs", latencyMs);
        status.put("circuitState", breaker.getState().name());
        status.put("lastFailure", lastFailureByService.get(serviceName));
        status.put("checkedAt", Instant.now().toString());
        return status;
    }

    private String probeDatabase() {
        try {
            Integer probe = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(probe) ? "UP" : "DEGRADED";
        } catch (DataAccessException ex) {
            throw new IllegalStateException("Database probe failed", ex);
        }
    }

    private String probeRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            return pong != null && pong.equalsIgnoreCase("PONG") ? "UP" : "DEGRADED";
        } catch (Exception ex) {
            throw new IllegalStateException("Redis probe failed", ex);
        }
    }

    private String probePayment() {
        boolean hasKeyId = razorpayKeyId != null && !razorpayKeyId.isBlank();
        boolean hasSecret = razorpayKeySecret != null && !razorpayKeySecret.isBlank();
        if (hasKeyId && hasSecret) {
            return "UP";
        }
        if (hasKeyId) {
            return "DEGRADED";
        }
        throw new IllegalStateException("Razorpay credentials are missing");
    }

    private String probeNotification() {
        boolean hasSmtp = smtpHost != null && !smtpHost.isBlank();
        boolean hasResend = resendApiKey != null && !resendApiKey.isBlank();
        if (hasSmtp && hasResend) {
            return "UP";
        }
        if (hasSmtp || hasResend) {
            return "DEGRADED";
        }
        throw new IllegalStateException("Notification providers are not configured");
    }
}

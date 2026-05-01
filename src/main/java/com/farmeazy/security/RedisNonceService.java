package com.farmeazy.security;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisNonceService {

    private static final Logger logger = LoggerFactory.getLogger(RedisNonceService.class);

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisNonceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Try to record a nonce for the given client. Returns true if the nonce was not present and was recorded,
     * false if it was already present (replay).
     */
    public boolean recordNonce(String clientId, String nonce, long ttlMillis) {
        try {
            if (clientId == null || nonce == null) return false;
            String key = buildKey(clientId, nonce);
            Boolean set = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMillis(Math.max(1000, ttlMillis)));
            return Boolean.TRUE.equals(set);
        } catch (Exception ex) {
            logger.warn("Redis nonce check failed, allowing request as fallback: {}", ex.getMessage());
            return true; // Fail-open: if Redis is unavailable, do not block requests here
        }
    }

    private String buildKey(String clientId, String nonce) {
        return "gateway:nonce:" + clientId + ":" + nonce;
    }
}

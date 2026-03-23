package com.farmeazy.service;

import com.farmeazy.entity.User;
import com.farmeazy.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
public class NotificationSseService {
    private static final Logger log = LoggerFactory.getLogger(NotificationSseService.class);
    private static final java.util.Set<String> ADMIN_ROLES = java.util.Set.of("ADMIN", "SUPERADMIN");

    private final Map<String, EmitterInfo> emitters = new java.util.concurrent.ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final UserRepository userRepository;

    // last payload sent (JSON string) for debugging
    private volatile String lastPayloadJson = "";

    // token -> token metadata
    private final Map<String, TokenInfo> tokenStore = Collections.synchronizedMap(new HashMap<>());

    public NotificationSseService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private static class TokenInfo {
        final String username;
        final long expiryEpochMillis;

        TokenInfo(String username, long expiryEpochMillis) {
            this.username = username;
            this.expiryEpochMillis = expiryEpochMillis;
        }
    }

    private static class EmitterInfo {
        final String id;
        final SseEmitter emitter;
        final String owner;
        final long createdAt;
        volatile long lastSentAt;

        EmitterInfo(String id, SseEmitter emitter, String owner) {
            this.id = id;
            this.emitter = emitter;
            this.owner = owner;
            this.createdAt = Instant.now().toEpochMilli();
            this.lastSentAt = 0L;
        }
    }

    public SseEmitter createEmitter(String ownerUsername) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        String id = UUID.randomUUID().toString();
        EmitterInfo info = new EmitterInfo(id, emitter, ownerUsername);
        emitters.put(id, info);

        emitter.onCompletion(() -> {
            emitters.remove(id);
            log.debug("SSE emitter completed (id={}), remaining emitters={}", id, emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(id);
            log.debug("SSE emitter timed out (id={}), remaining emitters={}", id, emitters.size());
        });
        emitter.onError((e) -> {
            emitters.remove(id);
            log.debug("SSE emitter error removed (id={}): {}", id, e == null ? "null" : e.getMessage());
        });

        return emitter;
    }

    public String createTokenForUser(String username, long ttlMillis) {
        cleanupExpiredTokens();
        String token = UUID.randomUUID().toString();
        long expiry = Instant.now().toEpochMilli() + ttlMillis;
        tokenStore.put(token, new TokenInfo(username, expiry));
        return token;
    }

    private void cleanupExpiredTokens() {
        long now = Instant.now().toEpochMilli();
        tokenStore.entrySet().removeIf(entry -> entry.getValue() == null || now > entry.getValue().expiryEpochMillis);
    }

    public java.util.Optional<String> validateAndConsumeToken(String token) {
        if (token == null || token.isBlank()) return java.util.Optional.empty();

        TokenInfo info = tokenStore.remove(token); // consume token so it cannot be reused
        if (info == null) return java.util.Optional.empty();
        if (Instant.now().toEpochMilli() > info.expiryEpochMillis) return java.util.Optional.empty();

        java.util.Optional<User> userOpt = userRepository.findByEmail(info.username);
        if (userOpt.isEmpty()) return java.util.Optional.empty();

        User user = userOpt.get();
        if (!Boolean.TRUE.equals(user.getActive())) return java.util.Optional.empty();

        boolean hasAdminRole = user.getRoles() != null && user.getRoles().stream().anyMatch(ADMIN_ROLES::contains);
        if (!hasAdminRole) return java.util.Optional.empty();

        return java.util.Optional.of(info.username);
    }

    public void sendNotifications(Object payload) {
        String json;
        try {
            json = mapper.writeValueAsString(payload);
            lastPayloadJson = json;
        } catch (Exception e) {
            json = "[]";
            lastPayloadJson = json;
        }

        log.debug("Sending notifications to {} emitter(s); payloadBytes={}", emitters.size(), json == null ? 0 : json.length());
        List<String> failed = new ArrayList<>();
        for (EmitterInfo info : emitters.values()) {
            try {
                info.emitter.send(SseEmitter.event().name("notifications").data(json));
                info.lastSentAt = Instant.now().toEpochMilli();
            } catch (IOException ex) {
                failed.add(info.id);
                log.debug("Removed emitter due to send error (id={}): {}", info.id, ex.getMessage());
            }
        }
        // remove failed emitters after iteration
        for (String id : failed) {
            emitters.remove(id);
        }
    }

    public int getEmitterCount() {
        return emitters.size();
    }

    public String getLastPayloadJson() {
        return lastPayloadJson;
    }

    public List<Map<String, Object>> getEmitterDetails() {
        List<Map<String, Object>> details = new ArrayList<>();
        for (EmitterInfo info : emitters.values()) {
            details.add(Map.of(
                    "id", info.id,
                    "owner", info.owner,
                    "createdAt", info.createdAt,
                    "lastSentAt", info.lastSentAt
            ));
        }
        return details;
    }
}

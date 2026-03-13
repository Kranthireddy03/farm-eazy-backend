package com.farmeazy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class NotificationSseService {
    private static final Logger log = LoggerFactory.getLogger(NotificationSseService.class);
    private final Map<String, EmitterInfo> emitters = new java.util.concurrent.ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    // last payload sent (JSON string) for debugging
    private volatile String lastPayloadJson = "";

    // token -> expiryEpochMillis
    private final Map<String, Long> tokenStore = Collections.synchronizedMap(new HashMap<>());

    private static class EmitterInfo {
        final String id;
        final SseEmitter emitter;
        final long createdAt;
        volatile long lastSentAt;

        EmitterInfo(String id, SseEmitter emitter) {
            this.id = id;
            this.emitter = emitter;
            this.createdAt = Instant.now().toEpochMilli();
            this.lastSentAt = 0L;
        }
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        String id = UUID.randomUUID().toString();
        EmitterInfo info = new EmitterInfo(id, emitter);
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
        String token = UUID.randomUUID().toString();
        long expiry = Instant.now().toEpochMilli() + ttlMillis;
        tokenStore.put(token, expiry);
        return token;
    }

    public boolean validateAndConsumeToken(String token) {
        if (token == null) return false;
        Long expiry = tokenStore.get(token);
        if (expiry == null) return false;
        if (Instant.now().toEpochMilli() > expiry) {
            tokenStore.remove(token);
            return false;
        }
        // consume token so it cannot be reused
        tokenStore.remove(token);
        return true;
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
                    "createdAt", info.createdAt,
                    "lastSentAt", info.lastSentAt
            ));
        }
        return details;
    }
}

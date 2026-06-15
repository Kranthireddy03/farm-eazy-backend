package com.farmeazy.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import com.farmeazy.security.ApiPayloadCryptoService;

@Configuration
public class RedisCacheConfig implements CachingConfigurer {

        private static final Logger logger = LoggerFactory.getLogger(RedisCacheConfig.class);

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory,
            ApiPayloadCryptoService cryptoService,
            @Value("${security.redis.cache.encrypt-values:true}") boolean encryptCacheValues) {
        RedisSerializer<Object> valueSerializer = new EncryptedJsonRedisSerializer(cryptoService, encryptCacheValues);

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .entryTtl(Duration.ofMinutes(10));

        Map<String, RedisCacheConfiguration> cacheTtls = new HashMap<>();
        // Support Tickets (short TTL due to high update frequency)
        cacheTtls.put("supportTicketList", baseConfig.entryTtl(Duration.ofMinutes(2)));
        cacheTtls.put("supportTicketAdminStats", baseConfig.entryTtl(Duration.ofMinutes(1)));
        cacheTtls.put("supportTicketUserStats", baseConfig.entryTtl(Duration.ofMinutes(1)));
        
        // Farm & Crop (moderate TTL)
        cacheTtls.put("farmById", baseConfig.entryTtl(Duration.ofMinutes(5)));
        cacheTtls.put("farmListByUser", baseConfig.entryTtl(Duration.ofMinutes(3)));
        cacheTtls.put("cropById", baseConfig.entryTtl(Duration.ofMinutes(5)));
        cacheTtls.put("cropListByUser", baseConfig.entryTtl(Duration.ofMinutes(3)));
        cacheTtls.put("cropListByFarm", baseConfig.entryTtl(Duration.ofMinutes(3)));
        
        // Products (longer TTL - less frequent updates)
        cacheTtls.put("productById", baseConfig.entryTtl(Duration.ofMinutes(10)));
        cacheTtls.put("productListActive", baseConfig.entryTtl(Duration.ofMinutes(8)));
        cacheTtls.put("productListByCategory", baseConfig.entryTtl(Duration.ofMinutes(8)));
        cacheTtls.put("productListSeller", baseConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Blog Posts (longer TTL - static content)
        cacheTtls.put("blogPostBySlug", baseConfig.entryTtl(Duration.ofMinutes(15)));
        cacheTtls.put("blogPostsPublic", baseConfig.entryTtl(Duration.ofMinutes(15)));
        cacheTtls.put("blogPostsAdmin", baseConfig.entryTtl(Duration.ofMinutes(10)));
        cacheTtls.put("blogPostsUser", baseConfig.entryTtl(Duration.ofMinutes(8)));
        
        // FAQ (longer TTL - static content)
        cacheTtls.put("faqApproved", baseConfig.entryTtl(Duration.ofMinutes(20)));
        cacheTtls.put("faqById", baseConfig.entryTtl(Duration.ofMinutes(20)));
        cacheTtls.put("faqAdmin", baseConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Orders (moderate TTL - business critical)
        cacheTtls.put("orderListUser", baseConfig.entryTtl(Duration.ofMinutes(3)));
        cacheTtls.put("orderById", baseConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Notifications (very short TTL - real-time updates)
        cacheTtls.put("notificationUser", baseConfig.entryTtl(Duration.ofMinutes(1)));
        
        // Dashboard Stats (short TTL - frequently viewed)
        cacheTtls.put("dashboardStats", baseConfig.entryTtl(Duration.ofMinutes(2)));
        
        // Delivery Locations (long TTL - rarely changes)
        cacheTtls.put("deliveryLocationAll", baseConfig.entryTtl(Duration.ofMinutes(30)));
        cacheTtls.put("deliveryLocationActive", baseConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Addresses (moderate TTL)
        cacheTtls.put("addressListUser", baseConfig.entryTtl(Duration.ofMinutes(5)));
        cacheTtls.put("addressById", baseConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Irrigation (moderate TTL)
        cacheTtls.put("irrigationById", baseConfig.entryTtl(Duration.ofMinutes(5)));
        cacheTtls.put("irrigationByFarm", baseConfig.entryTtl(Duration.ofMinutes(3)));
        cacheTtls.put("irrigationUpcoming", baseConfig.entryTtl(Duration.ofMinutes(2)));
        
        // Payouts (short TTL - financial data)
        cacheTtls.put("payoutListUser", baseConfig.entryTtl(Duration.ofMinutes(3)));
        cacheTtls.put("payoutPending", baseConfig.entryTtl(Duration.ofMinutes(2)));
        cacheTtls.put("payoutById", baseConfig.entryTtl(Duration.ofMinutes(5)));

                CacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                                .cacheDefaults(baseConfig)
                                .withInitialCacheConfigurations(cacheTtls)
                                .transactionAware()
                                .build();

                ConcurrentMapCacheManager fallbackCacheManager = new ConcurrentMapCacheManager(
                                "supportTicketList",
                                "supportTicketAdminStats",
                                "supportTicketUserStats",
                                "farmById",
                                "farmListByUser",
                                "cropById",
                                "cropListByUser",
                                "cropListByFarm",
                                "productById",
                                "productListActive",
                                "productListByCategory",
                                "productListSeller",
                                "blogPostBySlug",
                                "blogPostsPublic",
                                "blogPostsAdmin",
                                "blogPostsUser",
                                "faqApproved",
                                "faqById",
                                "faqAdmin",
                                "orderListUser",
                                "orderById",
                                "notificationUser",
                                "deliveryLocationAll",
                                "deliveryLocationActive",
                                "addressListUser",
                                "addressById",
                                "irrigationById",
                                "irrigationByFarm",
                                "irrigationUpcoming",
                                "payoutListUser",
                                "payoutPending",
                                "payoutById"
                );

                if (!isRedisAvailable(redisConnectionFactory)) {
                        logger.warn("REDIS_UNAVAILABLE_FALLBACK enabled=true message=Redis is unreachable, using in-memory cache manager");
                        return fallbackCacheManager;
                }

                CompositeCacheManager compositeCacheManager = new CompositeCacheManager(redisCacheManager, fallbackCacheManager);
                compositeCacheManager.setFallbackToNoOpCache(false);
                return compositeCacheManager;
        }

        private boolean isRedisAvailable(RedisConnectionFactory redisConnectionFactory) {
                try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                        String pingResponse = connection.ping();
                        return "PONG".equalsIgnoreCase(pingResponse);
                } catch (Exception exception) {
                        logger.warn("REDIS_CONNECTIVITY_CHECK_FAILED message={}", exception.getMessage());
                        return false;
                }
        }

        @Bean
        @Override
        public CacheErrorHandler errorHandler() {
                return new CacheErrorHandler() {
                        @Override
                        public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                                logger.warn("CACHE_GET_ERROR cache={} key={} message={}", cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                                logger.warn("CACHE_PUT_ERROR cache={} key={} message={}", cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                                logger.warn("CACHE_EVICT_ERROR cache={} key={} message={}", cache.getName(), key, exception.getMessage());
                        }

                        @Override
                        public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                                logger.warn("CACHE_CLEAR_ERROR cache={} message={}", cache.getName(), exception.getMessage());
                        }
                };
    }

        private static class EncryptedJsonRedisSerializer implements RedisSerializer<Object> {
                private final GenericJackson2JsonRedisSerializer delegate = new GenericJackson2JsonRedisSerializer();
                private final ApiPayloadCryptoService cryptoService;
                private final boolean encrypt;

                EncryptedJsonRedisSerializer(ApiPayloadCryptoService cryptoService, boolean encrypt) {
                        this.cryptoService = cryptoService;
                        this.encrypt = encrypt;
                }

                @Override
                public byte[] serialize(Object value) {
                        byte[] raw = delegate.serialize(value);
                        if (raw == null || raw.length == 0) {
                                return raw;
                        }
                        if (!encrypt) {
                                return raw;
                        }

                        String encrypted = cryptoService.encrypt(new String(raw, StandardCharsets.UTF_8));
                        return encrypted.getBytes(StandardCharsets.UTF_8);
                }

                @Override
                public Object deserialize(byte[] bytes) {
                        if (bytes == null || bytes.length == 0) {
                                return null;
                        }

                        if (!encrypt) {
                                return delegate.deserialize(bytes);
                        }

                        try {
                                String decrypted = cryptoService.decrypt(new String(bytes, StandardCharsets.UTF_8));
                                return delegate.deserialize(decrypted.getBytes(StandardCharsets.UTF_8));
                        } catch (Exception ignored) {
                                // Backward compatibility for entries stored before cache encryption was enabled.
                                return delegate.deserialize(bytes);
                        }
                }
        }
}

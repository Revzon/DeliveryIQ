package com.deliveryiq.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;
    private final ObjectMapper objectMapper;

    public CacheService(RedisTemplate<String, Object> redisTemplate, RedisConfig redisConfig, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.redisConfig = redisConfig;
        this.objectMapper = objectMapper;
    }

    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, long ttlSeconds) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.convertValue(cached, type);
            }
        } catch (Exception ex) {
            log.warn("Redis read failed for key {}: {}", key, ex.getMessage());
        }

        T value = loader.get();
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            log.warn("Redis write failed for key {}: {}", key, ex.getMessage());
        }
        return value;
    }

    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("Redis delete failed for key {}: {}", key, ex.getMessage());
        }
    }

    public void evictByPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("Redis prefix delete failed for {}: {}", prefix, ex.getMessage());
        }
    }

    public long dashboardTtlSeconds() {
        return redisConfig.getDashboardTtlSeconds();
    }

    public long routeTtlSeconds() {
        return redisConfig.getRouteTtlSeconds();
    }
}

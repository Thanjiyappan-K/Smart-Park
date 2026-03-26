package com.smartpark.parking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingAvailabilityCache {

    private static final String KEY_PREFIX = "parking:";
    private static final String KEY_SUFFIX = ":available_slots";

    private final StringRedisTemplate redisTemplate;

    @Value("${parking.cache.ttl-seconds:45}")
    private int ttlSeconds;

    public void setAvailableSlots(Long parkingId, int slots) {
        try {
            String key = KEY_PREFIX + parkingId + KEY_SUFFIX;
            redisTemplate.opsForValue().set(key, String.valueOf(slots), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Redis set failed (cache miss acceptable): {}", e.getMessage());
        }
    }

    public Integer getAvailableSlots(Long parkingId) {
        try {
            String key = KEY_PREFIX + parkingId + KEY_SUFFIX;
            String val = redisTemplate.opsForValue().get(key);
            return val != null ? Integer.parseInt(val) : null;
        } catch (Exception e) {
            log.debug("Redis get failed (fallback to DB): {}", e.getMessage());
            return null;
        }
    }

    public void evict(Long parkingId) {
        try {
            String key = KEY_PREFIX + parkingId + KEY_SUFFIX;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.debug("Redis evict failed: {}", e.getMessage());
        }
    }

    public void evictAll() {
        try {
            var keys = redisTemplate.keys(KEY_PREFIX + "*" + KEY_SUFFIX);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.debug("Redis evictAll failed: {}", e.getMessage());
        }
    }
}

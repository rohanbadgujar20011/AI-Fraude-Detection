package com.frauddetection.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Fast, in-memory (Redis) state for real-time fraud checks. Postgres holds
 * the durable history; Redis holds the "hot" rolling state we need to read
 * on every single transaction with sub-millisecond latency.
 *
 * Keys used (all namespaced per user):
 *   user:{id}:txn_count_1min    -> INCR + TTL 60s, powers velocity check
 *   user:{id}:avg_amount        -> running average, powers amount-spike check
 *   user:{id}:last_location     -> last seen location, powers impossible-travel check
 *   user:{id}:last_location_time -> epoch timestamp of last seen location
 *   user:{id}:last_device       -> last seen device, powers new-device check
 */
@Service
public class UserProfileService {

    private final StringRedisTemplate redis;

    private static final Duration VELOCITY_WINDOW = Duration.ofMinutes(1);

    public UserProfileService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public long incrementAndGetVelocity(String userId) {
        String key = velocityKey(userId);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, VELOCITY_WINDOW);
        }
        return count == null ? 0 : count;
    }

    public BigDecimal getRollingAverage(String userId) {
        String raw = redis.opsForValue().get(avgKey(userId));
        return raw == null ? null : new BigDecimal(raw);
    }

    /**
     * Simple exponential moving average, weight 0.3 to new value. Cheap,
     * no need to store full history in Redis just to compute this.
     */
    public void updateRollingAverage(String userId, BigDecimal newAmount) {
        BigDecimal current = getRollingAverage(userId);
        BigDecimal updated = current == null
                ? newAmount
                : current.multiply(BigDecimal.valueOf(0.7))
                    .add(newAmount.multiply(BigDecimal.valueOf(0.3)));
        redis.opsForValue().set(avgKey(userId), updated.toPlainString());
    }

    public String getLastLocation(String userId) {
        return redis.opsForValue().get(locationKey(userId));
    }

    public Long getLastLocationTime(String userId) {
        String raw = redis.opsForValue().get(locationTimeKey(userId));
        return raw == null ? null : Long.parseLong(raw);
    }

    public void setLastLocation(String userId, String location, long epochSecond) {
        redis.opsForValue().set(locationKey(userId), location);
        redis.opsForValue().set(locationTimeKey(userId), String.valueOf(epochSecond));
    }

    public String getLastDevice(String userId) {
        return redis.opsForValue().get(deviceKey(userId));
    }

    public void setLastDevice(String userId, String deviceId) {
        redis.opsForValue().set(deviceKey(userId), deviceId);
    }

    private String velocityKey(String userId)     { return "user:%s:txn_count_1min".formatted(userId); }
    private String avgKey(String userId)          { return "user:%s:avg_amount".formatted(userId); }
    private String locationKey(String userId)     { return "user:%s:last_location".formatted(userId); }
    private String locationTimeKey(String userId) { return "user:%s:last_location_time".formatted(userId); }
    private String deviceKey(String userId)       { return "user:%s:last_device".formatted(userId); }
}

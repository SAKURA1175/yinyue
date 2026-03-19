package com.yinyue.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RequestRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RequestRateLimiter.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final AppSecurityProperties securityProperties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Map<String, AtomicInteger> inMemoryCounters = new ConcurrentHashMap<>();

    public RequestRateLimiter(AppSecurityProperties securityProperties,
                              ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.securityProperties = securityProperties;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public boolean allow(String key) {
        if (!securityProperties.isRateLimitEnabled() || securityProperties.getRateLimitPerMinute() <= 0) {
            return true;
        }

        String bucket = String.valueOf(Instant.now().getEpochSecond() / WINDOW.getSeconds());
        String limiterKey = "rate-limit:" + key + ":" + bucket;

        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                Long current = redisTemplate.opsForValue().increment(limiterKey);
                if (current != null && current == 1L) {
                    redisTemplate.expire(limiterKey, WINDOW);
                }
                return current != null && current <= securityProperties.getRateLimitPerMinute();
            }
        } catch (Exception e) {
            log.warn("Redis rate limit unavailable, falling back to in-memory limiter: {}", e.getMessage());
        }

        pruneOldBuckets(bucket);
        AtomicInteger counter = inMemoryCounters.computeIfAbsent(limiterKey, ignored -> new AtomicInteger(0));
        return counter.incrementAndGet() <= securityProperties.getRateLimitPerMinute();
    }

    private void pruneOldBuckets(String activeBucket) {
        if (inMemoryCounters.size() < 1000) {
            return;
        }

        Iterator<String> iterator = inMemoryCounters.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!key.endsWith(activeBucket)) {
                iterator.remove();
            }
        }
    }
}

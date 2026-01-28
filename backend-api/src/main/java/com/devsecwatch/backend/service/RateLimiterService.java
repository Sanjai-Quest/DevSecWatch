package com.devsecwatch.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm");

    public boolean allowRequest(String userId) {
        String key = "rate_limit:" + userId + ":" + LocalDateTime.now().format(FORMATTER);
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        return count != null && count <= MAX_REQUESTS_PER_MINUTE;
    }

    public int getRemainingAttempts(String userId) {
        String key = "rate_limit:" + userId + ":" + LocalDateTime.now().format(FORMATTER);
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        return Math.max(0, MAX_REQUESTS_PER_MINUTE - count);
    }
}

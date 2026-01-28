package com.devsecwatch.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastResetTimes = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_HOUR = 50;
    private static final long HOUR_IN_MILLIS = 60 * 60 * 1000;

    public boolean allowRequest(String userId) {
        long currentTime = System.currentTimeMillis();

        // Check if we need to reset the counter
        lastResetTimes.compute(userId, (key, lastReset) -> {
            if (lastReset == null || currentTime - lastReset > HOUR_IN_MILLIS) {
                requestCounts.put(userId, new AtomicInteger(0));
                return currentTime;
            }
            return lastReset;
        });

        // Increment and check
        AtomicInteger count = requestCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        return count.incrementAndGet() <= MAX_REQUESTS_PER_HOUR;
    }

    public int getRemainingRequests(String userId) {
        AtomicInteger count = requestCounts.get(userId);
        if (count == null)
            return MAX_REQUESTS_PER_HOUR;
        return Math.max(0, MAX_REQUESTS_PER_HOUR - count.get());
    }
}

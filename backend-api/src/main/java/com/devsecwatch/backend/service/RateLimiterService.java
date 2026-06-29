package com.devsecwatch.backend.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final ProxyManager<byte[]> proxyManager;
    private final com.devsecwatch.backend.repository.UserRepository userRepository;

    private static final String SCAN_RATE_LIMIT_PREFIX = "rate_limit:scans:";

    public boolean allowRequest(String username) {
        return userRepository.findByUsername(username)
                .map(user -> resolveBucket(user.getId()).tryConsume(1))
                .orElse(true);
    }

    public io.github.bucket4j.ConsumptionProbe tryConsumeAndReturnProbe(String username) {
        return userRepository.findByUsername(username)
                .map(user -> resolveBucket(user.getId()).tryConsumeAndReturnRemaining(1))
                .orElse(null);
    }

    private Bucket resolveBucket(Long userId) {
        String key = SCAN_RATE_LIMIT_PREFIX + userId;
        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillIntervally(5, Duration.ofMinutes(10))
                        .build())
                .build();
        return proxyManager.builder().build(key.getBytes(), configSupplier);
    }
}

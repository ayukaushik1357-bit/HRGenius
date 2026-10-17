package com.hrgenius.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final long windowSeconds;
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Instant>> ipAttempts = new ConcurrentHashMap<>();

    public LoginRateLimiter() {
        this(10, 60); // Default: 10 attempts per minute per IP
    }

    public LoginRateLimiter(int maxAttempts, long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    public boolean tryAcquire(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(windowSeconds);

        ConcurrentLinkedDeque<Instant> timestamps = ipAttempts.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        // Evict expired timestamps
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxAttempts) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    public void reset() {
        ipAttempts.clear();
    }
}

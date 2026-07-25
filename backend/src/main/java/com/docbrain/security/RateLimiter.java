package com.docbrain.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class RateLimiter {

    private final int maxRequests;
    private final long windowMs;
    private final Map<UUID, ConcurrentLinkedDeque<Long>> requestLog = new ConcurrentHashMap<>();

    public RateLimiter(@Value("${docbrain.rate-limit.max-requests:20}") int maxRequests,
                       @Value("${docbrain.rate-limit.window-seconds:60}") int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowMs = windowSeconds * 1000L;
    }

    public boolean isAllowed(UUID userId) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> timestamps = requestLog.computeIfAbsent(userId,
                k -> new ConcurrentLinkedDeque<>());

        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMs) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }
}

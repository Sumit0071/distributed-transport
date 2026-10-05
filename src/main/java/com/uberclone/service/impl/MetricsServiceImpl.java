package com.uberclone.service.impl;

import com.uberclone.service.MetricsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsServiceImpl implements MetricsService {

    private final AtomicLong totalRidesCreated = new AtomicLong(0);
    private final AtomicLong totalRidesCompleted = new AtomicLong(0);
    private final AtomicLong totalPaymentsCreated = new AtomicLong(0);
    private final AtomicLong paymentFailures = new AtomicLong(0);
    private final AtomicLong retryAttempts = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong successfulMatches = new AtomicLong(0);
    private final AtomicLong retryMatches = new AtomicLong(0);
    private final AtomicLong surgeApplied = new AtomicLong(0);
    private final AtomicLong driverTimeouts = new AtomicLong(0);
    private final AtomicLong cancelledRides = new AtomicLong(0);
    private final AtomicLong noDriverFound = new AtomicLong(0);
    private final AtomicLong redisFailures = new AtomicLong(0);
    private final AtomicLong circuitBreakerOpen = new AtomicLong(0);
    private final AtomicLong totalMatchingTimeMs = new AtomicLong(0);
    private final AtomicLong matchingCount = new AtomicLong(0);
    private final AtomicLong retrySuccessCount = new AtomicLong(0);

    @Override
    public void recordMatchingLatency(long ms) {
        totalMatchingTimeMs.addAndGet(ms);
        matchingCount.incrementAndGet();
    }

    @Override
    public void incrementRedisFailure() {
        redisFailures.incrementAndGet();
    }

    @Override
    public void setCircuitBreakerState(boolean open) {
        circuitBreakerOpen.set(open ? 1 : 0);
    }

    @Override
    public void incrementRetrySuccess() {
        retrySuccessCount.incrementAndGet();
    }

    @Override
    public void incrementTotalRidesCreated() {
        totalRidesCreated.incrementAndGet();
    }

    @Override
    public void incrementTotalRidesCompleted() {
        totalRidesCompleted.incrementAndGet();
    }

    @Override
    public void incrementTotalPaymentsCreated() {
        totalPaymentsCreated.incrementAndGet();
    }

    @Override
    public void incrementPaymentFailures() {
        paymentFailures.incrementAndGet();
    }

    @Override
    public void incrementRetryAttempts() {
        retryAttempts.incrementAndGet();
    }

    @Override
    public void incrementCacheHits() {
        cacheHits.incrementAndGet();
    }

    @Override
    public void incrementCacheMisses() {
        cacheMisses.incrementAndGet();
    }

    @Override
    public void incrementSuccessfulMatches() {
        successfulMatches.incrementAndGet();
    }

    @Override
    public void incrementRetryMatches() {
        retryMatches.incrementAndGet();
    }

    @Override
    public void incrementSurgeApplied() {
        surgeApplied.incrementAndGet();
    }

    @Override
    public void incrementDriverTimeouts() {
        driverTimeouts.incrementAndGet();
    }

    @Override
    public void incrementCancelledRides() {
        cancelledRides.incrementAndGet();
    }

    @Override
    public void incrementNoDriverFound() {
        noDriverFound.incrementAndGet();
    }

    @Override
    public Map<String, Long> getMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("total_rides_created", totalRidesCreated.get());
        metrics.put("total_rides_completed", totalRidesCompleted.get());
        metrics.put("total_payments_created", totalPaymentsCreated.get());
        metrics.put("payment_failures", paymentFailures.get());
        metrics.put("retry_attempts", retryAttempts.get());
        metrics.put("cache_hits", cacheHits.get());
        metrics.put("cache_misses", cacheMisses.get());
        metrics.put("successful_matches", successfulMatches.get());
        metrics.put("retry_matches", retryMatches.get());
        metrics.put("surge_applied", surgeApplied.get());
        metrics.put("driver_timeouts", driverTimeouts.get());
        metrics.put("cancelled_rides", cancelledRides.get());
        metrics.put("no_driver_found", noDriverFound.get());
        metrics.put("redis_failures", redisFailures.get());
        metrics.put("circuit_breaker_open", circuitBreakerOpen.get());
        metrics.put("retry_success_rate", matchingCount.get() == 0 ? 0 : (retrySuccessCount.get() * 100 / matchingCount.get()));
        metrics.put("avg_matching_latency_ms", matchingCount.get() == 0 ? 0 : (totalMatchingTimeMs.get() / matchingCount.get()));
        return metrics;
    }
}

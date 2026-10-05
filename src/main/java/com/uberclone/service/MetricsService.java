package com.uberclone.service;

import java.util.Map;

public interface MetricsService {
    void incrementTotalRidesCreated();
    void incrementTotalRidesCompleted();
    void incrementTotalPaymentsCreated();
    void incrementPaymentFailures();
    void incrementRetryAttempts();
    void incrementCacheHits();
    void incrementCacheMisses();
    void incrementSuccessfulMatches();
    void incrementRetryMatches();
    void incrementSurgeApplied();
    void incrementDriverTimeouts();
    void incrementCancelledRides();
    void incrementNoDriverFound();
    void recordMatchingLatency(long ms);
    void incrementRedisFailure();
    void setCircuitBreakerState(boolean open);
    void incrementRetrySuccess();
    Map<String, Long> getMetrics();
}

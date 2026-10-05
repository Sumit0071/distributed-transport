package com.uberclone.worker;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.uberclone.domain.Ride;
import com.uberclone.domain.enums.RideStatus;
import com.uberclone.repository.RideRepository;
import com.uberclone.service.MetricsService;
import com.uberclone.service.impl.RideServiceImpl;

import jakarta.annotation.PreDestroy;

@Component
public class RetryMatchingWorker {

    private static final Logger log = LoggerFactory.getLogger(RetryMatchingWorker.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RideServiceImpl rideService;
    private final RideRepository rideRepository;
    private final MetricsService metricsService;
    private volatile boolean isShuttingDown = false;

    private static final long[] RETRY_DELAYS_SEC = {2, 5, 10, 20, 40};

    public RetryMatchingWorker(RedisTemplate<String, Object> redisTemplate, 
                               RideServiceImpl rideService, 
                               RideRepository rideRepository, 
                               MetricsService metricsService) {
        this.redisTemplate = redisTemplate;
        this.rideService = rideService;
        this.rideRepository = rideRepository;
        this.metricsService = metricsService;
    }

    @Scheduled(fixedDelay = 1000)
    public void processRetryQueue() {
        if (isShuttingDown) return;

        for (int i = 0; i < 10; i++) {
            if (isShuttingDown) break;

            Object value = redisTemplate.opsForList().leftPop("queue:ride:retry");
            if (value == null) break;

            UUID rideId = UUID.fromString(value.toString());
            String retryCountKey = "ride:retry:count:" + rideId;
            String nextRetryTimeKey = "ride:retry:time:" + rideId;

            Object nextRetryTimeObj = redisTemplate.opsForValue().get(nextRetryTimeKey);
            if (nextRetryTimeObj != null) {
                long nextRetryTime = Long.parseLong(nextRetryTimeObj.toString());
                if (System.currentTimeMillis() < nextRetryTime) {
                    redisTemplate.opsForList().rightPush("queue:ride:retry", rideId.toString());
                    continue;
                }
            }

            log.info("RETRY MATCH -> ride={}", rideId);

            boolean matched;
            try {
                matched = rideService.retryMatchRide(rideId);
            } catch (Exception e) {
                log.warn("RETRY ERROR -> ride={} error={}", rideId, e.getMessage());
                requeueWithBackoff(rideId, retryCountKey, nextRetryTimeKey);
                continue;
            }

            if (matched) {
                handleSuccess(rideId, retryCountKey, nextRetryTimeKey);
            } else {
                requeueWithBackoff(rideId, retryCountKey, nextRetryTimeKey);
            }
        }
    }

    private void handleSuccess(UUID rideId, String retryCountKey, String nextRetryTimeKey) {
        metricsService.incrementRetryMatches();
        redisTemplate.delete(retryCountKey);
        redisTemplate.delete(nextRetryTimeKey);
        log.info("RETRY SUCCESS -> ride={}", rideId);
    }

    private void requeueWithBackoff(UUID rideId, String retryCountKey, String nextRetryTimeKey) {
        Long count = redisTemplate.opsForValue().increment(retryCountKey);
        if (count == null) count = 1L;

        if (count > RETRY_DELAYS_SEC.length) {
            handleMaxRetriesExceeded(rideId, retryCountKey, nextRetryTimeKey);
        } else {
            long delaySec = RETRY_DELAYS_SEC[count.intValue() - 1];
            long nextRetryTimestamp = System.currentTimeMillis() + (delaySec * 1000);
            
            redisTemplate.opsForValue().set(nextRetryTimeKey, String.valueOf(nextRetryTimestamp), 10, TimeUnit.MINUTES);
            redisTemplate.opsForList().rightPush("queue:ride:retry", rideId.toString());
            log.info("RETRY QUEUED -> ride={} attempt={} nextRetryIn={}s", rideId, count, delaySec);
        }
    }

    private void handleMaxRetriesExceeded(UUID rideId, String retryCountKey, String nextRetryTimeKey) {
        log.warn("RETRY MAX EXCEEDED -> ride={}", rideId);
        
        redisTemplate.opsForList().rightPush("queue:ride:dead", rideId.toString());
        
        Ride ride = rideRepository.findById(rideId).orElse(null);
        if (ride != null && ride.getStatus() == RideStatus.REQUESTED) {
            ride.setStatus(RideStatus.CANCELLED);
            ride.setCompletedAt(LocalDateTime.now());
            rideRepository.save(ride);
            metricsService.incrementNoDriverFound();
            log.info("RETRY FAILED FINAL -> ride={}", rideId);
        }

        redisTemplate.delete(retryCountKey);
        redisTemplate.delete(nextRetryTimeKey);
    }

    @PreDestroy
    public void shutdown() {
        log.info("WORKER SHUTDOWN CLEAN");
        isShuttingDown = true;
    }
}

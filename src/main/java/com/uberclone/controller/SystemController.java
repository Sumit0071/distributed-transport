package com.uberclone.controller;

import com.uberclone.service.HealthCheckService;
import com.uberclone.service.MetricsService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MetricsService metricsService;
    private final HealthCheckService healthCheckService;

    public SystemController(RedisTemplate<String, Object> redisTemplate, 
                            MetricsService metricsService, 
                            HealthCheckService healthCheckService) {
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.putAll(healthCheckService.checkHealth());
        health.put("metrics", metricsService.getMetrics());
        
        Long retryQueueSize = redisTemplate.opsForList().size("queue:ride:retry");
        Long deadQueueSize = redisTemplate.opsForList().size("queue:ride:dead");
        health.put("queue_retry_size", retryQueueSize != null ? retryQueueSize : 0);
        health.put("queue_dead_size", deadQueueSize != null ? deadQueueSize : 0);
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/market-stats")
    public ResponseEntity<Map<String, Object>> getMarketStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_demand", getLongValue("metric:demand:total"));
        stats.put("total_supply", getLongValue("metric:supply:total"));
        stats.put("successful_matches", getLongValue("metric:matches:success"));
        stats.put("retry_matches", getLongValue("metric:matches:retry"));
        stats.put("failed_matches", getLongValue("metric:matches:failed"));
        stats.put("surge_events", getLongValue("metric:surge:applied"));

        Set<Object> hotspots = redisTemplate.opsForSet().members("hotspots:active");
        stats.put("active_hotspots", hotspots != null ? hotspots : Collections.emptySet());

        return ResponseEntity.ok(stats);
    }

    private long getLongValue(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) return 0L;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return 0L;
        }
    }
}

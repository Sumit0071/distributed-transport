package com.uberclone.service.impl;

import com.uberclone.service.SurgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SurgeServiceImpl implements SurgeService {

    private static final Logger log = LoggerFactory.getLogger(SurgeServiceImpl.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public SurgeServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public double calculateSurgeMultiplier(double pickupLat, double pickupLng) {
        String cellId = getCellId(pickupLat, pickupLng);
        long demand = getLongValue("area:demand:" + cellId);
        long supply = getLongValue("area:supply:" + cellId);

        double ratio = supply <= 0 ? (double) demand : (double) demand / Math.max(supply, 1);
        double surge = 1.0;
        if (ratio > 5.0) surge = 3.0;
        else if (ratio > 3.0) surge = 2.0;
        else if (ratio > 2.0) surge = 1.5;
        else if (ratio > 1.0) surge = 1.2;

        if (surge > 1.0) log.info("SURGE CALCULATED -> cell={}, demand={}, supply={}, surge={}", cellId, demand, supply, surge);
        return surge;
    }

    private String getCellId(double lat, double lng) {
        return (long) Math.floor(lat * 100) + ":" + (long) Math.floor(lng * 100);
    }

    private long getLongValue(String key) {
        Object val = redisTemplate.opsForValue().get(key);
        if (val == null) return 0;
        return val instanceof Number n ? n.longValue() : Long.parseLong(val.toString());
    }
}

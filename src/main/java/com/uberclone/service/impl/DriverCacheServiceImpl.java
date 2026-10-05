package com.uberclone.service.impl;

import com.uberclone.domain.DriverProfile;
import com.uberclone.dto.DriverLocationDTO;
import com.uberclone.repository.DriverProfileRepository;
import com.uberclone.service.DriverCacheService;
import com.uberclone.service.MetricsService;
import com.uberclone.util.DistanceCalculator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DriverCacheServiceImpl implements DriverCacheService {

    private static final Logger log = LoggerFactory.getLogger(DriverCacheServiceImpl.class);
    private static final String DRIVER_KEY_PREFIX = "driver:location:";
    private static final String DRIVER_GEO_KEY = "drivers:geo";

    private final RedisTemplate<String, Object> redisTemplate;
    private final DistanceCalculator distanceCalculator;
    private final MetricsService metricsService;
    private final DriverProfileRepository driverProfileRepository;

    public DriverCacheServiceImpl(RedisTemplate<String, Object> redisTemplate, 
                                 DistanceCalculator distanceCalculator, 
                                 MetricsService metricsService, 
                                 DriverProfileRepository driverProfileRepository) {
        this.redisTemplate = redisTemplate;
        this.distanceCalculator = distanceCalculator;
        this.metricsService = metricsService;
        this.driverProfileRepository = driverProfileRepository;
    }

    @PostConstruct
    public void warmupCache() {
        try {
            List<DriverProfile> allDrivers = driverProfileRepository.findAll();
            int cached = 0;

            for (DriverProfile driver : allDrivers) {
                UUID driverId = driver.getUser().getId();
                Boolean available = driver.getIsAvailable();

                if (driver.getCurrentLatitude() == null || driver.getCurrentLongitude() == null) continue;

                if (Boolean.FALSE.equals(available)) {
                    removeDriver(driverId);
                    continue;
                }

                updateDriverLocation(driverId, driver.getCurrentLatitude(), driver.getCurrentLongitude(), true);
                cached++;
            }
            log.info("Cache warmup completed. Drivers cached = {}", cached);
        } catch (Exception e) {
            log.error("Cache warmup failed: {}", e.getMessage());
        }
    }

    @Override
    public void updateDriverLocation(UUID driverId, double lat, double lng, boolean isAvailable) {
        try {
            String key = DRIVER_KEY_PREFIX + driverId;
            Map<Object, Object> existing = redisTemplate.opsForHash().entries(key);
            Double previousLat = null;
            Double previousLng = null;

            if (existing != null && !existing.isEmpty()) {
                Object latObj = existing.get("lat");
                Object lngObj = existing.get("lng");
                if (latObj != null && lngObj != null) {
                    previousLat = Double.parseDouble(latObj.toString());
                    previousLng = Double.parseDouble(lngObj.toString());
                }
            }

            redisTemplate.opsForHash().put(key, "lat", String.valueOf(lat));
            redisTemplate.opsForHash().put(key, "lng", String.valueOf(lng));
            redisTemplate.opsForHash().put(key, "id", driverId.toString());
            redisTemplate.persist(key);

            if (!isAvailable) {
                redisTemplate.opsForZSet().remove(DRIVER_GEO_KEY, driverId.toString());
                return;
            }

            boolean shouldUpdateGeo = true;
            if (previousLat != null && previousLng != null) {
                double movedKm = distanceCalculator.calculateDistance(previousLat, previousLng, lat, lng);
                if (movedKm * 1000 < 20.0) shouldUpdateGeo = false;
            }

            if (shouldUpdateGeo) {
                redisTemplate.opsForGeo().add(DRIVER_GEO_KEY, new Point(lng, lat), driverId.toString());
            }
        } catch (Exception e) {
            log.error("Redis write failed for driver {}: {}", driverId, e.getMessage());
        }
    }

    @Override
    public List<DriverLocationDTO> getNearbyDrivers(double lat, double lng, double radiusKm) {
        try {
            Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, org.springframework.data.geo.Metrics.KILOMETERS));
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending();
            var results = redisTemplate.opsForGeo().radius(DRIVER_GEO_KEY, circle, args);

            if (results == null || results.getContent().isEmpty()) {
                metricsService.incrementCacheMisses();
                return Collections.emptyList();
            }

            List<DriverLocationDTO> drivers = results.getContent().stream()
                    .map(result -> {
                        String driverId = (String) result.getContent().getName();
                        double dist = result.getDistance().getValue();
                        try {
                           String key = DRIVER_KEY_PREFIX + driverId;
                           Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                           if (data == null || data.isEmpty()) {
                               redisTemplate.opsForZSet().remove(DRIVER_GEO_KEY, driverId);
                               return null;
                           }
                           return DriverLocationDTO.builder()
                                   .driverId(UUID.fromString(driverId))
                                   .latitude(Double.parseDouble(data.get("lat").toString()))
                                   .longitude(Double.parseDouble(data.get("lng").toString()))
                                   .distance(dist)
                                   .build();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!drivers.isEmpty()) metricsService.incrementCacheHits();
            else metricsService.incrementCacheMisses();

            return drivers;
        } catch (Exception e) {
            log.error("Redis read failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void removeDriver(UUID driverId) {
        redisTemplate.delete(DRIVER_KEY_PREFIX + driverId);
        redisTemplate.opsForZSet().remove(DRIVER_GEO_KEY, driverId.toString());
    }
}

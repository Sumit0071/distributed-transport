package com.uberclone.service.impl;

import com.uberclone.domain.DriverProfile;
import com.uberclone.dto.DriverLocationDTO;
import com.uberclone.repository.DriverProfileRepository;
import com.uberclone.service.MetricsService;
import com.uberclone.util.DistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DriverCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private GeoOperations<String, Object> geoOperations;
    @Mock
    private DistanceCalculator distanceCalculator;
    @Mock
    private MetricsService metricsService;
    @Mock
    private DriverProfileRepository driverProfileRepository;

    private DriverCacheServiceImpl driverCacheService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        driverCacheService = new DriverCacheServiceImpl(redisTemplate, distanceCalculator, metricsService, driverProfileRepository);
    }

    @Test
    void updateDriverLocation_ShouldStoreStringsInRedis() {
        UUID driverId = UUID.randomUUID();
        double lat = 40.7128;
        double lng = -74.0060;

        driverCacheService.updateDriverLocation(driverId, lat, lng, true);

        verify(hashOperations).put(contains(driverId.toString()), eq("lat"), eq(String.valueOf(lat)));
        verify(hashOperations).put(contains(driverId.toString()), eq("lng"), eq(String.valueOf(lng)));
        verify(hashOperations).put(contains(driverId.toString()), eq("id"), eq(driverId.toString()));
    }

    @Test
    void getNearbyDrivers_ShouldParseStringValuesCorrectly() {
        UUID driverId = UUID.randomUUID();
        String key = "driver:location:" + driverId;
        double lat = 40.7128;
        double lng = -74.0060;
        double searchLat = 40.7306;
        double searchLng = -73.9352;
        double distance = 3.5;

        Map<Object, Object> data = new HashMap<>();
        data.put("id", driverId.toString());
        data.put("lat", String.valueOf(lat));
        data.put("lng", String.valueOf(lng));

        when(hashOperations.entries(key)).thenReturn(data);
        RedisGeoCommands.GeoLocation<Object> location =
                new RedisGeoCommands.GeoLocation<>(driverId.toString(), null);
        GeoResult<RedisGeoCommands.GeoLocation<Object>> geoResult =
                new GeoResult<>(location, new Distance(distance, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<Object>> geoResults =
                new GeoResults<>(Collections.singletonList(geoResult));

        when(geoOperations.radius(eq("drivers:geo"),
                any(org.springframework.data.geo.Circle.class),
                any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(geoResults);

        List<DriverLocationDTO> result = driverCacheService.getNearbyDrivers(searchLat, searchLng, 5.0);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(driverId, result.get(0).getDriverId());
        assertEquals(lat, result.get(0).getLatitude());
        assertEquals(lng, result.get(0).getLongitude());
        
        verify(metricsService).incrementCacheHits();
    }

    @Test
    void getNearbyDrivers_ShouldHandleEmptyCache() {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> emptyResults =
                new GeoResults<>(Collections.emptyList());
        when(geoOperations.radius(eq("drivers:geo"),
                any(org.springframework.data.geo.Circle.class),
                any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(emptyResults);

        List<DriverLocationDTO> result = driverCacheService.getNearbyDrivers(0.0, 0.0, 5.0);

        assertTrue(result.isEmpty());
        verify(metricsService).incrementCacheMisses();
    }
}

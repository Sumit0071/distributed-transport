package com.uberclone.service;

import com.uberclone.dto.DriverLocationDTO;

import java.util.List;
import java.util.UUID;

public interface DriverCacheService {
    void updateDriverLocation(UUID driverId, double lat, double lng, boolean isAvailable);
    List<DriverLocationDTO> getNearbyDrivers(double lat, double lng, double radiusKm);
    void removeDriver(UUID driverId);
}

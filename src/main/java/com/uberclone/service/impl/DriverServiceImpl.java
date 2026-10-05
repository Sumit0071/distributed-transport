package com.uberclone.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uberclone.domain.DriverProfile;
import com.uberclone.dto.DriverLocationDTO;
import com.uberclone.dto.DriverLocationUpdateDTO;
import com.uberclone.exception.BusinessException;
import com.uberclone.repository.DriverProfileRepository;
import com.uberclone.service.DriverCacheService;
import com.uberclone.service.DriverService;
import com.uberclone.util.DistanceCalculator;

@Service
public class DriverServiceImpl implements DriverService {

    private final DriverProfileRepository driverProfileRepository;
    private final DistanceCalculator distanceCalculator;
    private final DriverCacheService driverCacheService;

    public DriverServiceImpl(DriverProfileRepository driverProfileRepository, 
                             DistanceCalculator distanceCalculator, 
                             DriverCacheService driverCacheService) {
        this.driverProfileRepository = driverProfileRepository;
        this.distanceCalculator = distanceCalculator;
        this.driverCacheService = driverCacheService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverLocationDTO> findNearestAvailableDrivers(double lat, double lng) {
        List<DriverLocationDTO> cachedDrivers = driverCacheService.getNearbyDrivers(lat, lng, 50.0);
        if (cachedDrivers != null && !cachedDrivers.isEmpty()) {
            return cachedDrivers;
        }

        List<DriverProfile> availableDrivers = driverProfileRepository.findByIsAvailableTrue();
        return availableDrivers.stream()
                .map(driver -> {
                    double distance = distanceCalculator.calculateDistance(
                            lat, lng,
                            driver.getCurrentLatitude(), driver.getCurrentLongitude()
                    );
                    return DriverLocationDTO.builder()
                            .driverId(driver.getUser().getId())
                            .driverName(driver.getUser().getName())
                            .latitude(driver.getCurrentLatitude())
                            .longitude(driver.getCurrentLongitude())
                            .distance(distance)
                            .rating(driver.getRating())
                            .build();
                })
                .sorted(Comparator.comparingDouble(DriverLocationDTO::getDistance))
                .limit(5)
                .toList();
    }

    @Override
    @Transactional
    public void updateDriverLocation(DriverLocationUpdateDTO updateDTO) {
        UUID driverId = updateDTO.getDriverId();
        DriverProfile driverProfile = driverProfileRepository.findByUserId(driverId)
                .orElseThrow(() -> new BusinessException("Driver not found"));

        driverProfile.setCurrentLatitude(updateDTO.getLatitude());
        driverProfile.setCurrentLongitude(updateDTO.getLongitude());
        driverProfileRepository.save(driverProfile);

        boolean isAvailable = Boolean.TRUE.equals(driverProfile.getIsAvailable());
        driverCacheService.updateDriverLocation(driverId, updateDTO.getLatitude(), updateDTO.getLongitude(), isAvailable);
    }
}

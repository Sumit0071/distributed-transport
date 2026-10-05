package com.uberclone.service.impl;

import com.uberclone.service.EtaService;
import com.uberclone.util.DistanceCalculator;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class EtaServiceImpl implements EtaService {

    private static final double AVERAGE_SPEED_KMH = 28.0;
    private final DistanceCalculator distanceCalculator;

    public EtaServiceImpl(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public double estimateArrivalMinutes(double dLat, double dLng, double rLat, double rLng) {
        double distanceKm = distanceCalculator.calculateDistance(dLat, dLng, rLat, rLng);
        double baseHours = distanceKm / AVERAGE_SPEED_KMH;
        int hour = LocalTime.now().getHour();
        double multiplier = ((hour >= 8 && hour <= 10) || (hour >= 17 && hour <= 20)) ? 1.4 : 1.0;
        return baseHours * 60.0 * multiplier;
    }
}

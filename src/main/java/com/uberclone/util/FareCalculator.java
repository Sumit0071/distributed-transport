package com.uberclone.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FareCalculator {

    @Value("${app.ride.base-fare-rate:2.50}")
    private double baseFareRate;

    public double calculateFare(double distanceKm, double surgeMultiplier) {
        double fare = distanceKm * baseFareRate * surgeMultiplier;
        return Math.round(fare * 100.0) / 100.0;
    }

    public BigDecimal calculateFareAsBigDecimal(double distanceKm) {
        BigDecimal fare = BigDecimal.valueOf(distanceKm * baseFareRate);
        return fare.setScale(2, RoundingMode.HALF_UP);
    }
}

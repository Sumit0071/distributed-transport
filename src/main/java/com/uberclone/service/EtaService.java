package com.uberclone.service;

public interface EtaService {
    double estimateArrivalMinutes(double driverLat, double driverLng, double riderLat, double riderLng);
}


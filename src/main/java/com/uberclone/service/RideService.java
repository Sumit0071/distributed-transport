package com.uberclone.service;

import com.uberclone.dto.RideRequestDTO;
import com.uberclone.dto.RideResponseDTO;

import java.util.UUID;

public interface RideService {
    RideResponseDTO createRideRequest(RideRequestDTO requestDTO, String idempotencyKey);
    RideResponseDTO acceptRide(UUID rideId, UUID driverId);
    RideResponseDTO startRide(UUID rideId);
    RideResponseDTO completeRide(UUID rideId);
    RideResponseDTO getRideById(UUID rideId);
    RideResponseDTO cancelRide(UUID rideId);
}

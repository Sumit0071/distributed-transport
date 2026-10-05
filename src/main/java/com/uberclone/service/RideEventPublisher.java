package com.uberclone.service;

import com.uberclone.dto.RideResponseDTO;

public interface RideEventPublisher {
    void publishRideAccepted(RideResponseDTO ride);
    void publishRideStarted(RideResponseDTO ride);
    void publishRideCompleted(RideResponseDTO ride);
}

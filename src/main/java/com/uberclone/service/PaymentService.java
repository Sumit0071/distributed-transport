package com.uberclone.service;

import com.uberclone.domain.Ride;
import com.uberclone.dto.PaymentDTO;

import java.util.UUID;

public interface PaymentService {
    void processPayment(Ride ride);
    PaymentDTO getPaymentByRideId(UUID rideId);
}

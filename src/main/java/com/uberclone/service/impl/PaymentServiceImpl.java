package com.uberclone.service.impl;

import com.uberclone.domain.Payment;
import com.uberclone.domain.Ride;
import com.uberclone.dto.PaymentDTO;
import com.uberclone.exception.BusinessException;
import com.uberclone.repository.PaymentRepository;
import com.uberclone.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public void processPayment(Ride ride) {
        if (paymentRepository.findByRideId(ride.getId()).isPresent()) return;
        Payment payment = Payment.builder()
                .ride(ride)
                .amount(ride.getFare())
                .status("COMPLETED")
                .paymentMethod("CREDIT_CARD")
                .build();
        paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentByRideId(UUID rideId) {
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new BusinessException("Payment not found for ride: " + rideId));
        return PaymentDTO.builder()
                .id(payment.getId())
                .rideId(payment.getRide().getId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}

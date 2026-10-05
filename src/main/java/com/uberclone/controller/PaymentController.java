package com.uberclone.controller;

import com.uberclone.dto.PaymentDTO;
import com.uberclone.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<PaymentDTO> getPaymentByRideId(@PathVariable UUID rideId) {
        return ResponseEntity.ok(paymentService.getPaymentByRideId(rideId));
    }
}

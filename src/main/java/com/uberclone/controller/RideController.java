package com.uberclone.controller;

import com.uberclone.dto.RideRequestDTO;
import com.uberclone.dto.RideResponseDTO;
import com.uberclone.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping("/request")
    public ResponseEntity<RideResponseDTO> requestRide(
            @Valid @RequestBody RideRequestDTO requestDTO,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(rideService.createRideRequest(requestDTO, idempotencyKey));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<RideResponseDTO> acceptRide(
            @PathVariable UUID id,
            @RequestParam UUID driverId) {
        return ResponseEntity.ok(rideService.acceptRide(id, driverId));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<RideResponseDTO> startRide(@PathVariable UUID id) {
        return ResponseEntity.ok(rideService.startRide(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<RideResponseDTO> completeRide(@PathVariable UUID id) {
        return ResponseEntity.ok(rideService.completeRide(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<RideResponseDTO> cancelRide(@PathVariable UUID id) {
        return ResponseEntity.ok(rideService.cancelRide(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RideResponseDTO> getRide(@PathVariable UUID id) {
        return ResponseEntity.ok(rideService.getRideById(id));
    }
}

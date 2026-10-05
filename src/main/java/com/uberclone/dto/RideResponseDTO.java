package com.uberclone.dto;

import com.uberclone.domain.enums.RideStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class RideResponseDTO {
    private UUID id;
    private UUID riderId;
    private UUID driverId;
    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;
    private RideStatus status;
    private BigDecimal fare;
    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public RideResponseDTO() {
    }

    public RideResponseDTO(UUID id, UUID riderId, UUID driverId, Double pickupLat, Double pickupLng, Double dropoffLat, Double dropoffLng, RideStatus status, BigDecimal fare, LocalDateTime requestedAt, LocalDateTime acceptedAt, LocalDateTime startedAt, LocalDateTime completedAt) {
        this.id = id;
        this.riderId = riderId;
        this.driverId = driverId;
        this.pickupLat = pickupLat;
        this.pickupLng = pickupLng;
        this.dropoffLat = dropoffLat;
        this.dropoffLng = dropoffLng;
        this.status = status;
        this.fare = fare;
        this.requestedAt = requestedAt;
        this.acceptedAt = acceptedAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public static Builder builder() { return new Builder(); }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRiderId() { return riderId; }
    public void setRiderId(UUID riderId) { this.riderId = riderId; }
    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }
    public Double getPickupLat() { return pickupLat; }
    public void setPickupLat(Double pickupLat) { this.pickupLat = pickupLat; }
    public Double getPickupLng() { return pickupLng; }
    public void setPickupLng(Double pickupLng) { this.pickupLng = pickupLng; }
    public Double getDropoffLat() { return dropoffLat; }
    public void setDropoffLat(Double dropoffLat) { this.dropoffLat = dropoffLat; }
    public Double getDropoffLng() { return dropoffLng; }
    public void setDropoffLng(Double dropoffLng) { this.dropoffLng = dropoffLng; }
    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus status) { this.status = status; }
    public BigDecimal getFare() { return fare; }
    public void setFare(BigDecimal fare) { this.fare = fare; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public static class Builder {
        private UUID id;
        private UUID riderId;
        private UUID driverId;
        private Double pickupLat;
        private Double pickupLng;
        private Double dropoffLat;
        private Double dropoffLng;
        private RideStatus status;
        private BigDecimal fare;
        private LocalDateTime requestedAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder riderId(UUID riderId) { this.riderId = riderId; return this; }
        public Builder driverId(UUID driverId) { this.driverId = driverId; return this; }
        public Builder pickupLat(Double pickupLat) { this.pickupLat = pickupLat; return this; }
        public Builder pickupLng(Double pickupLng) { this.pickupLng = pickupLng; return this; }
        public Builder dropoffLat(Double dropoffLat) { this.dropoffLat = dropoffLat; return this; }
        public Builder dropoffLng(Double dropoffLng) { this.dropoffLng = dropoffLng; return this; }
        public Builder status(RideStatus status) { this.status = status; return this; }
        public Builder fare(BigDecimal fare) { this.fare = fare; return this; }
        public Builder requestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; return this; }
        public Builder acceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; return this; }
        public Builder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }

        public RideResponseDTO build() {
            return new RideResponseDTO(id, riderId, driverId, pickupLat, pickupLng, dropoffLat, dropoffLng, status, fare, requestedAt, acceptedAt, startedAt, completedAt);
        }
    }
}

package com.uberclone.domain;

import com.uberclone.domain.enums.RideStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "rides", indexes = {
        @Index(name = "idx_ride_rider_id", columnList = "rider_id"),
        @Index(name = "idx_ride_driver_id", columnList = "driver_id"),
        @Index(name = "idx_ride_status", columnList = "status")
})
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    @Column(name = "pickup_latitude", nullable = false)
    private Double pickupLatitude;

    @Column(name = "pickup_longitude", nullable = false)
    private Double pickupLongitude;

    @Column(name = "dropoff_latitude", nullable = false)
    private Double dropoffLatitude;

    @Column(name = "dropoff_longitude", nullable = false)
    private Double dropoffLongitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RideStatus status;

    private BigDecimal fare;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Ride() {
    }

    public Ride(UUID id, User rider, User driver, Double pickupLatitude, Double pickupLongitude, 
                Double dropoffLatitude, Double dropoffLongitude, RideStatus status, BigDecimal fare, 
                LocalDateTime requestedAt, LocalDateTime acceptedAt, LocalDateTime startedAt, 
                LocalDateTime completedAt, LocalDateTime createdAt) {
        this.id = id;
        this.rider = rider;
        this.driver = driver;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.dropoffLatitude = dropoffLatitude;
        this.dropoffLongitude = dropoffLongitude;
        this.status = status;
        this.fare = fare;
        this.requestedAt = requestedAt;
        this.acceptedAt = acceptedAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getRider() { return rider; }
    public void setRider(User rider) { this.rider = rider; }
    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }
    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }
    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }
    public Double getDropoffLatitude() { return dropoffLatitude; }
    public void setDropoffLatitude(Double dropoffLatitude) { this.dropoffLatitude = dropoffLatitude; }
    public Double getDropoffLongitude() { return dropoffLongitude; }
    public void setDropoffLongitude(Double dropoffLongitude) { this.dropoffLongitude = dropoffLongitude; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ride ride = (Ride) o;
        return Objects.equals(id, ride.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Builder {
        private UUID id;
        private User rider;
        private User driver;
        private Double pickupLatitude;
        private Double pickupLongitude;
        private Double dropoffLatitude;
        private Double dropoffLongitude;
        private RideStatus status;
        private BigDecimal fare;
        private LocalDateTime requestedAt;
        private LocalDateTime acceptedAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder rider(User rider) { this.rider = rider; return this; }
        public Builder driver(User driver) { this.driver = driver; return this; }
        public Builder pickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; return this; }
        public Builder pickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; return this; }
        public Builder dropoffLatitude(Double dropoffLatitude) { this.dropoffLatitude = dropoffLatitude; return this; }
        public Builder dropoffLongitude(Double dropoffLongitude) { this.dropoffLongitude = dropoffLongitude; return this; }
        public Builder status(RideStatus status) { this.status = status; return this; }
        public Builder fare(BigDecimal fare) { this.fare = fare; return this; }
        public Builder requestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; return this; }
        public Builder acceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; return this; }
        public Builder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public Builder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Ride build() {
            return new Ride(id, rider, driver, pickupLatitude, pickupLongitude, dropoffLatitude, dropoffLongitude, status, fare, requestedAt, acceptedAt, startedAt, completedAt, createdAt);
        }
    }
}

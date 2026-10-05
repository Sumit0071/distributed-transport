package com.uberclone.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "driver_profiles", indexes = {
        @Index(name = "idx_driver_user_id", columnList = "user_id"),
        @Index(name = "idx_driver_available", columnList = "is_available"),
        @Index(name = "idx_driver_location", columnList = "current_latitude, current_longitude")
})
public class DriverProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "vehicle_details", nullable = false)
    private String vehicleDetails;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Column(nullable = false)
    private Double rating = 5.0;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public DriverProfile() {
    }

    public DriverProfile(UUID id, User user, String vehicleDetails, Boolean isAvailable, 
                         Double currentLatitude, Double currentLongitude, Double rating, 
                         LocalDateTime lastUpdated) {
        this.id = id;
        this.user = user;
        this.vehicleDetails = vehicleDetails;
        this.isAvailable = isAvailable != null ? isAvailable : true;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.rating = rating != null ? rating : 5.0;
        this.lastUpdated = lastUpdated;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getVehicleDetails() { return vehicleDetails; }
    public void setVehicleDetails(String vehicleDetails) { this.vehicleDetails = vehicleDetails; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean available) { isAvailable = available; }
    public Double getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(Double currentLatitude) { this.currentLatitude = currentLatitude; }
    public Double getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(Double currentLongitude) { this.currentLongitude = currentLongitude; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DriverProfile that = (DriverProfile) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

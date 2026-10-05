package com.uberclone.dto;

import java.util.UUID;

public class DriverLocationDTO {
    private UUID driverId;
    private String driverName;
    private Double latitude;
    private Double longitude;
    private Double distance;
    private Double rating;

    public DriverLocationDTO() {
    }

    public DriverLocationDTO(UUID driverId, String driverName, Double latitude, Double longitude, Double distance, Double rating) {
        this.driverId = driverId;
        this.driverName = driverName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.rating = rating;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public static class Builder {
        private UUID driverId;
        private String driverName;
        private Double latitude;
        private Double longitude;
        private Double distance;
        private Double rating;

        public Builder driverId(UUID driverId) {
            this.driverId = driverId;
            return this;
        }

        public Builder driverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder distance(Double distance) {
            this.distance = distance;
            return this;
        }

        public Builder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public DriverLocationDTO build() {
            return new DriverLocationDTO(driverId, driverName, latitude, longitude, distance, rating);
        }
    }
}

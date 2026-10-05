package com.uberclone.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_ride_id", columnList = "ride_id")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    private Ride ride;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String paymentMethod;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Payment() {
    }

    public Payment(UUID id, Ride ride, BigDecimal amount, String status, String paymentMethod, LocalDateTime createdAt) {
        this.id = id;
        this.ride = ride;
        this.amount = amount;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
    }

    public static Builder builder() { return new Builder(); }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Ride getRide() { return ride; }
    public void setRide(Ride ride) { this.ride = ride; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Builder {
        private UUID id;
        private Ride ride;
        private BigDecimal amount;
        private String status;
        private String paymentMethod;
        private LocalDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder ride(Ride ride) { this.ride = ride; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder paymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Payment build() {
            return new Payment(id, ride, amount, status, paymentMethod, createdAt);
        }
    }
}

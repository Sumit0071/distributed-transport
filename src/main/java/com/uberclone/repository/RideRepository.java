package com.uberclone.repository;

import com.uberclone.domain.Ride;
import com.uberclone.domain.enums.RideStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {
    List<Ride> findByRiderId(UUID riderId);
    List<Ride> findByDriverId(UUID driverId);
    List<Ride> findByStatus(RideStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Ride r WHERE r.id = :id")
    Optional<Ride> findByIdWithLock(@Param("id") UUID id);
}

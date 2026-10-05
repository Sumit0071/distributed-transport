package com.uberclone.service.impl;

import com.uberclone.domain.DriverProfile;
import com.uberclone.domain.Ride;
import com.uberclone.domain.User;
import com.uberclone.domain.enums.RideStatus;
import com.uberclone.domain.enums.Role;
import com.uberclone.dto.DriverLocationDTO;
import com.uberclone.dto.RideRequestDTO;
import com.uberclone.dto.RideResponseDTO;
import com.uberclone.exception.BusinessException;
import com.uberclone.repository.DriverProfileRepository;
import com.uberclone.repository.RideRepository;
import com.uberclone.repository.UserRepository;
import com.uberclone.service.*;
import com.uberclone.util.DistanceCalculator;
import com.uberclone.util.FareCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RideServiceImpl implements RideService {

    private static final Logger log = LoggerFactory.getLogger(RideServiceImpl.class);
    private static final double MAX_SEARCH_RADIUS_KM = 5.0;
    private static final int REDIS_FAILURE_THRESHOLD = 3;
    private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 30000;

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DistanceCalculator distanceCalculator;
    private final FareCalculator fareCalculator;
    private final DriverCacheService driverCacheService;
    private final PaymentService paymentService;
    private final RideEventPublisher rideEventPublisher;
    private final MetricsService metricsService;
    private final EtaService etaService;
    private final SurgeService surgeService;
    private final RedisTemplate<String, Object> redisTemplate;

    private final AtomicInteger redisFailureCount = new AtomicInteger(0);
    private final AtomicBoolean isCircuitOpen = new AtomicBoolean(false);
    private long circuitOpenTime = 0;

    public RideServiceImpl(RideRepository rideRepository, 
                           UserRepository userRepository, 
                           DriverProfileRepository driverProfileRepository, 
                           DistanceCalculator distanceCalculator, 
                           FareCalculator fareCalculator, 
                           DriverCacheService driverCacheService, 
                           PaymentService paymentService, 
                           RideEventPublisher rideEventPublisher, 
                           MetricsService metricsService, 
                           RedisTemplate<String, Object> redisTemplate, 
                           EtaService etaService, 
                           SurgeService surgeService) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.distanceCalculator = distanceCalculator;
        this.fareCalculator = fareCalculator;
        this.driverCacheService = driverCacheService;
        this.paymentService = paymentService;
        this.rideEventPublisher = rideEventPublisher;
        this.metricsService = metricsService;
        this.redisTemplate = redisTemplate;
        this.etaService = etaService;
        this.surgeService = surgeService;
    }

    @Override
    @Transactional
    public RideResponseDTO createRideRequest(RideRequestDTO requestDTO, String idempotencyKey) {
        if (idempotencyKey != null) {
            String key = "idempotency:ride:" + idempotencyKey;
            Object existingRideId = executeWithCircuitBreaker(() -> redisTemplate.opsForValue().get(key));
            if (existingRideId != null) {
                log.warn("IDEMPOTENT REQUEST REJECTED -> key={}", idempotencyKey);
                Ride ride = rideRepository.findById(UUID.fromString(existingRideId.toString()))
                        .orElseThrow(() -> new BusinessException("Ride not found for idempotency key"));
                return mapToResponseDTO(ride);
            }
        }

        User rider = userRepository.findById(requestDTO.getRiderId())
                .orElseThrow(() -> new BusinessException("Rider not found"));

        if (rider.getRole() != Role.RIDER) {
            throw new BusinessException("User is not a rider");
        }

        long startTime = System.currentTimeMillis();

        String cellId = getCellId(requestDTO.getPickupLat(), requestDTO.getPickupLng());
        incrementDemand(cellId);
        incrementMetric("metric:demand:total");

        double distanceKm = distanceCalculator.calculateDistance(
                requestDTO.getPickupLat(), requestDTO.getPickupLng(),
                requestDTO.getDropoffLat(), requestDTO.getDropoffLng()
        );

        double surgeMultiplier = surgeService.calculateSurgeMultiplier(
                requestDTO.getPickupLat(), requestDTO.getPickupLng()
        );

        if (surgeMultiplier > 1.0) {
            metricsService.incrementSurgeApplied();
            incrementMetric("metric:surge:applied");
        }

        BigDecimal totalFare = BigDecimal.valueOf(fareCalculator.calculateFare(distanceKm, surgeMultiplier));

        Ride ride = Ride.builder()
                .rider(rider)
                .pickupLatitude(requestDTO.getPickupLat())
                .pickupLongitude(requestDTO.getPickupLng())
                .dropoffLatitude(requestDTO.getDropoffLat())
                .dropoffLongitude(requestDTO.getDropoffLng())
                .status(RideStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .fare(totalFare)
                .build();

        assignNearestDriver(ride);

        if (ride.getDriver() != null) {
            metricsService.incrementSuccessfulMatches();
            incrementMetric("metric:matches:success");
        } else {
            log.warn("MATCH FAILED -> queued for retry ride={}", ride.getId());
            executeWithCircuitBreaker(() -> redisTemplate.opsForList().rightPush("queue:ride:retry", ride.getId().toString()));
        }

        Ride savedRide = rideRepository.save(ride);
        
        if (idempotencyKey != null) {
            String key = "idempotency:ride:" + idempotencyKey;
            executeWithCircuitBreaker(() -> {
                redisTemplate.opsForValue().set(key, savedRide.getId().toString(), 10, TimeUnit.MINUTES);
                return null;
            });
        }

        metricsService.incrementTotalRidesCreated();
        metricsService.recordMatchingLatency(System.currentTimeMillis() - startTime);
        
        return mapToResponseDTO(savedRide);
    }

    private void assignNearestDriver(Ride ride) {
        checkCircuitBreaker();
        
        if (!isCircuitOpen.get()) {
            try {
                List<DriverLocationDTO> nearbyDrivers = driverCacheService.getNearbyDrivers(
                        ride.getPickupLatitude(), ride.getPickupLongitude(), MAX_SEARCH_RADIUS_KM
                );

                if (nearbyDrivers != null && !nearbyDrivers.isEmpty()) {
                    List<DriverLocationDTO> rankedDrivers = nearbyDrivers.stream()
                            .limit(5)
                            .sorted((d1, d2) -> {
                                double eta1 = etaService.estimateArrivalMinutes(d1.getLatitude(), d1.getLongitude(), ride.getPickupLatitude(), ride.getPickupLongitude());
                                double eta2 = etaService.estimateArrivalMinutes(d2.getLatitude(), d2.getLongitude(), ride.getPickupLatitude(), ride.getPickupLongitude());
                                return Double.compare(eta1, eta2);
                            })
                            .toList();

                    for (DriverLocationDTO cachedDriver : rankedDrivers) {
                        UUID driverId = cachedDriver.getDriverId();
                        String lockKey = "driver:lock:" + driverId;
                        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(30));

                        if (Boolean.TRUE.equals(locked)) {
                            Optional<DriverProfile> driverProfileOpt = driverProfileRepository.findByUserId(driverId);
                            if (driverProfileOpt.isPresent() && driverProfileOpt.get().getIsAvailable()) {
                                assignDriverToRide(ride, driverProfileOpt.get());
                                return;
                            } else {
                                redisTemplate.delete(lockKey);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                handleRedisFailure(e);
            }
        }

        log.warn("FALLBACK DB MATCHING -> ride={}", ride.getId());
        List<DriverProfile> availableDrivers = driverProfileRepository.findByIsAvailableTrue();
        Optional<DriverProfile> bestDriver = availableDrivers.stream()
                .filter(driver -> distanceCalculator.calculateDistance(ride.getPickupLatitude(), ride.getPickupLongitude(), driver.getCurrentLatitude(), driver.getCurrentLongitude()) <= MAX_SEARCH_RADIUS_KM)
                .sorted((d1, d2) -> Double.compare(
                        distanceCalculator.calculateDistance(ride.getPickupLatitude(), ride.getPickupLongitude(), d1.getCurrentLatitude(), d1.getCurrentLongitude()),
                        distanceCalculator.calculateDistance(ride.getPickupLatitude(), ride.getPickupLongitude(), d2.getCurrentLatitude(), d2.getCurrentLongitude())
                ))
                .findFirst();

        bestDriver.ifPresent(driverProfile -> assignDriverToRide(ride, driverProfile));
    }

    private void assignDriverToRide(Ride ride, DriverProfile driverProfile) {
        ride.setDriver(driverProfile.getUser());
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());
        
        driverProfile.setIsAvailable(false);
        driverProfileRepository.save(driverProfile);
        updateSupply(driverProfile, false);
        
        driverCacheService.updateDriverLocation(driverProfile.getUser().getId(), driverProfile.getCurrentLatitude(), driverProfile.getCurrentLongitude(), false);
        
        executeWithCircuitBreaker(() -> redisTemplate.delete("driver:lock:" + driverProfile.getUser().getId()));
        
        rideEventPublisher.publishRideAccepted(mapToResponseDTO(ride));
    }

    @Override
    @Transactional
    public RideResponseDTO acceptRide(UUID rideId, UUID driverId) {
        Ride ride = rideRepository.findByIdWithLock(rideId).orElseThrow(() -> new BusinessException("Ride not found"));
        if (ride.getStatus() != RideStatus.REQUESTED) throw new BusinessException("Ride already processed");

        User driverUser = userRepository.findById(driverId).orElseThrow(() -> new BusinessException("Driver not found"));
        DriverProfile driverProfile = driverProfileRepository.findByUserId(driverId).orElseThrow(() -> new BusinessException("Driver profile not found"));

        if (!driverProfile.getIsAvailable()) throw new BusinessException("Driver not available");

        ride.setDriver(driverUser);
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(LocalDateTime.now());
        
        driverProfile.setIsAvailable(false);
        driverProfileRepository.save(driverProfile);
        updateSupply(driverProfile, false);
        
        driverCacheService.updateDriverLocation(driverId, driverProfile.getCurrentLatitude(), driverProfile.getCurrentLongitude(), false);

        Ride savedRide = rideRepository.save(ride);
        rideEventPublisher.publishRideAccepted(mapToResponseDTO(savedRide));
        
        return mapToResponseDTO(savedRide);
    }

    @Override
    @Transactional
    public RideResponseDTO startRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new BusinessException("Ride not found"));
        if (ride.getStatus() != RideStatus.ACCEPTED) throw new BusinessException("Ride cannot be started");

        ride.setStatus(RideStatus.STARTED);
        ride.setStartedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);
        rideEventPublisher.publishRideStarted(mapToResponseDTO(savedRide));
        
        return mapToResponseDTO(savedRide);
    }

    @Override
    @Transactional
    public RideResponseDTO completeRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new BusinessException("Ride not found"));
        if (ride.getStatus() != RideStatus.STARTED) throw new BusinessException("Ride not started");

        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());

        DriverProfile driverProfile = driverProfileRepository.findByUserId(ride.getDriver().getId()).orElseThrow(() -> new BusinessException("Driver profile not found"));
        driverProfile.setIsAvailable(true);
        driverProfile.setCurrentLatitude(ride.getDropoffLatitude());
        driverProfile.setCurrentLongitude(ride.getDropoffLongitude());
        driverProfileRepository.save(driverProfile);
        updateSupply(driverProfile, true);
        
        driverCacheService.updateDriverLocation(driverProfile.getUser().getId(), driverProfile.getCurrentLatitude(), driverProfile.getCurrentLongitude(), true);

        Ride savedRide = rideRepository.save(ride);
        try {
            paymentService.processPayment(savedRide);
        } catch (Exception e) {
            metricsService.incrementPaymentFailures();
            throw e;
        }
        
        metricsService.incrementTotalRidesCompleted();
        rideEventPublisher.publishRideCompleted(mapToResponseDTO(savedRide));
        
        return mapToResponseDTO(savedRide);
    }

    @Override
    @Transactional
    public RideResponseDTO cancelRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new BusinessException("Ride not found"));
        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.STARTED) throw new BusinessException("Cannot cancel");

        if (ride.getDriver() != null) {
            DriverProfile driverProfile = driverProfileRepository.findByUserId(ride.getDriver().getId()).orElseThrow(() -> new BusinessException("Driver profile not found"));
            driverProfile.setIsAvailable(true);
            driverProfileRepository.save(driverProfile);
            updateSupply(driverProfile, true);
            driverCacheService.updateDriverLocation(driverProfile.getUser().getId(), driverProfile.getCurrentLatitude(), driverProfile.getCurrentLongitude(), true);
            executeWithCircuitBreaker(() -> redisTemplate.delete("driver:lock:" + driverProfile.getUser().getId()));
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setCompletedAt(LocalDateTime.now());
        Ride savedRide = rideRepository.save(ride);

        metricsService.incrementCancelledRides();
        return mapToResponseDTO(savedRide);
    }

    @Override
    @Transactional(readOnly = true)
    public RideResponseDTO getRideById(UUID rideId) {
        return mapToResponseDTO(rideRepository.findById(rideId).orElseThrow(() -> new BusinessException("Ride not found")));
    }

    public boolean retryMatchRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new BusinessException("Ride not found"));
        if (ride.getDriver() != null || ride.getStatus() != RideStatus.REQUESTED) return true;

        assignNearestDriver(ride);
        if (ride.getDriver() != null) {
            rideRepository.save(ride);
            metricsService.incrementRetrySuccess();
            return true;
        }
        return false;
    }

    private String getCellId(double lat, double lng) {
        return (long) Math.floor(lat * 100) + ":" + (long) Math.floor(lng * 100);
    }

    private void incrementDemand(String cellId) {
        executeWithCircuitBreaker(() -> {
            String demandKey = "area:demand:" + cellId;
            redisTemplate.opsForValue().increment(demandKey);
            redisTemplate.expire(demandKey, Duration.ofMinutes(5));
            updateHotspot(cellId);
            return null;
        });
    }

    private void updateSupply(DriverProfile profile, boolean available) {
        if (profile.getCurrentLatitude() == null) return;
        String cellId = getCellId(profile.getCurrentLatitude(), profile.getCurrentLongitude());
        executeWithCircuitBreaker(() -> {
            String supplyKey = "area:supply:" + cellId;
            redisTemplate.opsForValue().increment(supplyKey, available ? 1 : -1);
            redisTemplate.expire(supplyKey, Duration.ofMinutes(5));
            updateHotspot(cellId);
            if (available) incrementMetric("metric:supply:total");
            return null;
        });
    }

    private void updateHotspot(String cellId) {
        String demandKey = "area:demand:" + cellId;
        String supplyKey = "area:supply:" + cellId;
        long demand = getLongValue(demandKey);
        long supply = getLongValue(supplyKey);
        
        String hotspotsKey = "hotspots:active";
        boolean isHotspot = demand >= 5 && supply <= 2;
        if (isHotspot) redisTemplate.opsForSet().add(hotspotsKey, cellId);
        else redisTemplate.opsForSet().remove(hotspotsKey, cellId);
    }

    private long getLongValue(String key) {
        Object val = executeWithCircuitBreaker(() -> redisTemplate.opsForValue().get(key));
        if (val == null) return 0;
        return val instanceof Number n ? n.longValue() : Long.parseLong(val.toString());
    }

    private void incrementMetric(String key) {
        executeWithCircuitBreaker(() -> redisTemplate.opsForValue().increment(key));
    }

    private <T> T executeWithCircuitBreaker(java.util.function.Supplier<T> action) {
        checkCircuitBreaker();
        if (isCircuitOpen.get()) return null;
        try {
            T result = action.get();
            redisFailureCount.set(0);
            return result;
        } catch (Exception e) {
            handleRedisFailure(e);
            return null;
        }
    }

    private void checkCircuitBreaker() {
        if (isCircuitOpen.get() && System.currentTimeMillis() - circuitOpenTime > CIRCUIT_BREAKER_TIMEOUT_MS) {
            isCircuitOpen.set(false);
            metricsService.setCircuitBreakerState(false);
            log.info("CIRCUIT CLOSED -> Redis restored");
        }
    }

    private void handleRedisFailure(Exception e) {
        log.error("Redis error: {}", e.getMessage());
        metricsService.incrementRedisFailure();
        if (redisFailureCount.incrementAndGet() >= REDIS_FAILURE_THRESHOLD) {
            isCircuitOpen.set(true);
            circuitOpenTime = System.currentTimeMillis();
            metricsService.setCircuitBreakerState(true);
            log.error("CIRCUIT OPEN -> Redis disabled temporarily");
        }
    }

    private RideResponseDTO mapToResponseDTO(Ride ride) {
        return RideResponseDTO.builder()
                .id(ride.getId())
                .riderId(ride.getRider().getId())
                .driverId(ride.getDriver() != null ? ride.getDriver().getId() : null)
                .pickupLat(ride.getPickupLatitude())
                .pickupLng(ride.getPickupLongitude())
                .dropoffLat(ride.getDropoffLatitude())
                .dropoffLng(ride.getDropoffLongitude())
                .status(ride.getStatus())
                .fare(ride.getFare())
                .requestedAt(ride.getRequestedAt())
                .acceptedAt(ride.getAcceptedAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                .build();
    }
}

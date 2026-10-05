package com.uberclone.config;

import com.uberclone.domain.DriverProfile;
import com.uberclone.domain.User;
import com.uberclone.domain.enums.Role;
import com.uberclone.repository.DriverProfileRepository;
import com.uberclone.repository.UserRepository;
import com.uberclone.service.DriverCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverCacheService driverCacheService;

    public DataSeeder(UserRepository userRepository, 
                      DriverProfileRepository driverProfileRepository, 
                      DriverCacheService driverCacheService) {
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.driverCacheService = driverCacheService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            syncCache();
            return;
        }

        User rider = new User();
        rider.setName("John Rider");
        rider.setEmail("rider@test.com");
        rider.setPhone("+15550101");
        rider.setPasswordHash("hashed_password");
        rider.setRole(Role.RIDER);
        userRepository.save(rider);

        User driver = new User();
        driver.setName("Jane Driver");
        driver.setEmail("driver@test.com");
        driver.setPhone("+15550102");
        driver.setPasswordHash("hashed_password");
        driver.setRole(Role.DRIVER);
        userRepository.save(driver);

        DriverProfile profile = new DriverProfile();
        profile.setUser(driver);
        profile.setVehicleDetails("Toyota Prius - Black - XYZ 123");
        profile.setIsAvailable(true);
        profile.setCurrentLatitude(40.7128);
        profile.setCurrentLongitude(-74.0060);
        profile.setRating(4.9);
        profile.setLastUpdated(LocalDateTime.now());
        driverProfileRepository.save(profile);

        syncCache();
    }

    private void syncCache() {
        var drivers = driverProfileRepository.findByIsAvailableTrue();
        for (DriverProfile d : drivers) {
            if (d.getCurrentLatitude() != null && d.getCurrentLongitude() != null) {
                driverCacheService.updateDriverLocation(
                        d.getUser().getId(),
                        d.getCurrentLatitude(),
                        d.getCurrentLongitude(),
                        true
                );
            }
        }
    }
}

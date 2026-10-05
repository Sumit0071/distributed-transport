package com.uberclone.controller;

import com.uberclone.dto.DriverLocationDTO;
import com.uberclone.dto.DriverLocationUpdateDTO;
import com.uberclone.service.DriverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping("/nearest")
    public ResponseEntity<List<DriverLocationDTO>> findNearestDrivers(
            @RequestParam double lat,
            @RequestParam double lng) {
        return ResponseEntity.ok(driverService.findNearestAvailableDrivers(lat, lng));
    }

    @PostMapping("/location")
    public ResponseEntity<Void> updateDriverLocation(@RequestBody DriverLocationUpdateDTO request) {
        driverService.updateDriverLocation(request);
        return ResponseEntity.ok().build();
    }
}

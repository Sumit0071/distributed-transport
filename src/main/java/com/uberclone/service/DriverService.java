package com.uberclone.service;

import com.uberclone.dto.DriverLocationDTO;

import java.util.List;

public interface DriverService {
    List<DriverLocationDTO> findNearestAvailableDrivers(double lat, double lng);
    void updateDriverLocation(com.uberclone.dto.DriverLocationUpdateDTO updateDTO);
}

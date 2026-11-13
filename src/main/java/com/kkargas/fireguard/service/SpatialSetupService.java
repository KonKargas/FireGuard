package com.kkargas.fireguard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kkargas.fireguard.dto.LocationGeometry;
import com.kkargas.fireguard.enums.LocationType;
import com.kkargas.fireguard.entity.Location;

/** Creates a fully configured Location with its required temperature and light sensors. */
@Service
public class SpatialSetupService {
    private final LocationService locations;
    private final SensorService sensors;

    public SpatialSetupService(LocationService locations, SensorService sensors) {
        this.locations = locations;
        this.sensors = sensors;
    }

    @Transactional
    public Location createReadyLocation(Long floorId, String code, String name,
                                        String description, LocationGeometry geometry) {
        return createReadyLocation(floorId, code, name, description, geometry,
                LocationType.HOTEL_ROOM, true);
    }

    @Transactional
    public Location createReadyLocation(Long floorId, String code, String name,
                                        String description, LocationGeometry geometry,
                                        LocationType type, boolean installSensors) {
        Location location = locations.create(floorId, code, name, description, geometry, type);
        if (installSensors) sensors.installDefaultPair(location.getId());
        return location;
    }
}

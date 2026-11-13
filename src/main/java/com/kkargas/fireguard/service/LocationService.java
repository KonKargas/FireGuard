package com.kkargas.fireguard.service;

import com.kkargas.fireguard.util.TextFields;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kkargas.fireguard.dto.LocationGeometry;
import com.kkargas.fireguard.enums.LocationType;
import com.kkargas.fireguard.entity.Floor;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.repository.LocationRepository;
import com.kkargas.fireguard.repository.MeasurementRepository;
import com.kkargas.fireguard.repository.FireAlertRepository;
import com.kkargas.fireguard.repository.SensorRepository;

@Service
@Transactional(readOnly = true)
public class LocationService {
    private final LocationRepository locations;
    private final FloorService floors;
    private final SensorRepository sensors;
    private final MeasurementRepository measurements;
    private final FireAlertRepository alerts;

    public LocationService(LocationRepository locations, FloorService floors, SensorRepository sensors,
                           MeasurementRepository measurements, FireAlertRepository alerts) {
        this.locations = locations;
        this.floors = floors;
        this.sensors = sensors;
        this.measurements = measurements;
        this.alerts = alerts;
    }

    public Location get(Long id) {
        return locations.findById(Objects.requireNonNull(id, "Location ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
    }

    public List<Location> findByFloor(Long floorId) {
        floors.get(floorId);
        return locations.findByFloorIdOrderByCodeAsc(floorId);
    }

    public List<Location> findByBuilding(Long buildingId) {
        return locations.findByFloorBuildingIdOrderByFloorLevelNumberAscCodeAsc(buildingId);
    }

    @Transactional
    public Location create(Long floorId, String code, String name, String description,
                           LocationGeometry geometry) {
        return create(floorId, code, name, description, geometry, LocationType.HOTEL_ROOM);
    }

    @Transactional
    public Location create(Long floorId, String code, String name, String description,
                           LocationGeometry geometry, LocationType type) {
        Floor floor = floors.get(floorId);
        String normalizedCode = TextFields.required(code, "Location code", 20);
        if (locations.existsByFloorIdAndCode(floorId, normalizedCode)) {
            throw new IllegalArgumentException("Location code already exists on this floor");
        }
        validateNoOverlap(floorId, null, Objects.requireNonNull(geometry));
        Location location = new Location(normalizedCode,
                TextFields.required(name, "Location name", 100),
                TextFields.optional(description, 500), floor,
                geometry.x(), geometry.y(), geometry.z(),
                geometry.width(), geometry.depth(), geometry.height());
        location.setType(type == null ? LocationType.OTHER : type);
        return locations.save(location);
    }

    @Transactional
    public Location update(Long id, String code, String name, String description,
                           LocationGeometry geometry) {
        return update(id, code, name, description, geometry, null);
    }

    @Transactional
    public Location update(Long id, String code, String name, String description,
                           LocationGeometry geometry, LocationType type) {
        Location location = get(id);
        Long floorId = location.getFloor().getId();
        String normalizedCode = TextFields.required(code, "Location code", 20);
        if (!location.getCode().equals(normalizedCode)
                && locations.existsByFloorIdAndCode(floorId, normalizedCode)) {
            throw new IllegalArgumentException("Location code already exists on this floor");
        }
        LocationGeometry shape = Objects.requireNonNull(geometry);
        validateNoOverlap(floorId, id, shape);
        // Keep sensors inside the room after a resize.
        for (var sensor : sensors.findByLocationIdOrderByTypeAsc(id)) {
            if (!SensorService.isInside(shape, sensor.getPositionX(), sensor.getPositionY(), sensor.getPositionZ())) {
                throw new IllegalArgumentException("New room dimensions would place a sensor outside the room");
            }
        }
        if (type != null) location.setType(type);
        location.setCode(normalizedCode);
        location.setName(TextFields.required(name, "Location name", 100));
        location.setDescription(TextFields.optional(description, 500));
        location.setPosition(shape.x(), shape.y(), shape.z());
        location.setDimensions(shape.width(), shape.depth(), shape.height());
        return location;
    }

    private void validateNoOverlap(Long floorId, Long excludeId, LocationGeometry candidate) {
        for (Location existing : locations.findByFloorIdOrderByCodeAsc(floorId)) {
            if (excludeId != null && excludeId.equals(existing.getId())) continue;
            LocationGeometry other = new LocationGeometry(existing.getPositionX(), existing.getPositionY(),
                    existing.getPositionZ(), existing.getWidthMeters(), existing.getDepthMeters(),
                    existing.getHeightMeters());
            if (candidate.overlaps(other)) {
                throw new IllegalArgumentException("Space overlaps existing location: " + existing.getCode());
            }
        }
    }

    /** Permanently removes a location and all its dependent simulation data atomically. */
    @Transactional
    public void deleteWithDependencies(Long id) {
        Location location = get(id);
        // Foreign-key order matters: measurements -> sensors; alerts -> location.
        measurements.deleteByLocationId(id);
        sensors.deleteByLocationId(id);
        alerts.deleteByLocationId(id);
        locations.delete(location);
        locations.flush(); // Surface FK failures inside this transaction.
    }

    @Transactional(readOnly = true)
    public Location getDetails(Long locationId) {
        return locations.findDetailsById(locationId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Location not found: " + locationId
            ));
    }
}

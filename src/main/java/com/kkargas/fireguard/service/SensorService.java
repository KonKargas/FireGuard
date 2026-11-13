package com.kkargas.fireguard.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kkargas.fireguard.dto.LocationGeometry;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.entity.Sensor;
import com.kkargas.fireguard.enums.SensorType;
import com.kkargas.fireguard.repository.SensorRepository;
import com.kkargas.fireguard.repository.MeasurementRepository;

@Service
@Transactional(readOnly = true)
public class SensorService {
    private final SensorRepository sensors;
    private final LocationService locations;
    private final MeasurementRepository measurements;

    public SensorService(SensorRepository sensors, LocationService locations,
                         MeasurementRepository measurements) {
        this.sensors = sensors;
        this.locations = locations;
        this.measurements = measurements;
    }

    public List<Sensor> findByLocation(Long locationId) {
        locations.get(locationId);
        return sensors.findByLocationIdOrderByTypeAsc(locationId);
    }

    public Sensor get(Long sensorId) {
        return sensors.findById(Objects.requireNonNull(sensorId))
                .orElseThrow(() -> new IllegalArgumentException("Sensor not found: " + sensorId));
    }

    /** Creates BOTH required sensors in one transaction; no partial configuration. */
    @Transactional
    public List<Sensor> installDefaultPair(Long locationId) {
        Location room = locations.get(locationId);
        if (sensors.countByLocationId(locationId) != 0) {
            throw new IllegalStateException("Location already has sensors");
        }
        double width = room.getWidthMeters(), depth = room.getDepthMeters(), height = room.getHeightMeters();
        Sensor temp = new Sensor(code(locationId, SensorType.TEMPERATURE), SensorType.TEMPERATURE,
                room, width / 2, depth / 2, height * 0.9);
        Sensor light = new Sensor(code(locationId, SensorType.LUMINOSITY), SensorType.LUMINOSITY,
                room, width / 2, depth / 2, height * 0.6);
        if (sensors.existsByCodeIgnoreCase(temp.getCode()) || sensors.existsByCodeIgnoreCase(light.getCode())) {
            throw new IllegalStateException("Sensor code collision");
        }
        return sensors.saveAll(List.of(temp, light));
    }

    @Transactional
    public Sensor move(Long sensorId, double x, double y, double z) {
        Sensor sensor = get(sensorId);
        Location room = sensor.getLocation();
        LocationGeometry bounds = new LocationGeometry(room.getPositionX(), room.getPositionY(),
                room.getPositionZ(), room.getWidthMeters(), room.getDepthMeters(), room.getHeightMeters());
        if (!isInside(bounds, x, y, z)) {
            throw new IllegalArgumentException("Sensor coordinates outside room dimensions");
        }
        sensor.setPosition(x, y, z);
        return sensor;
    }

    // Sensors use LOCAL room coordinates, not floor coordinates.
    static boolean isInside(LocationGeometry room, double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && x >= 0 && x <= room.width()
                && y >= 0 && y <= room.depth()
                && z >= 0 && z <= room.height();
    }

    private static String code(Long locationId, SensorType type) {
        return "LOC-" + locationId + (type == SensorType.TEMPERATURE ? "-TEMP" : "-LIGHT");
    }

    @Transactional
    public void deleteUnused(Long sensorId) {
        Sensor sensor = get(sensorId);
        if (measurements.countBySensorId(sensorId) > 0) {
            throw new IllegalStateException("Cannot delete a sensor with measurement history");
        }
        sensors.delete(sensor);
    }
}

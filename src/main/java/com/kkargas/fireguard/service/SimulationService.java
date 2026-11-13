package com.kkargas.fireguard.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kkargas.fireguard.dto.SimulationResult;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.entity.Measurement;
import com.kkargas.fireguard.entity.Sensor;
import com.kkargas.fireguard.enums.SensorType;
import com.kkargas.fireguard.repository.LocationRepository;
import com.kkargas.fireguard.repository.MeasurementRepository;
import com.kkargas.fireguard.repository.SensorRepository;
import com.kkargas.fireguard.util.MeasurementGenerator;

/** Educational fire-detection simulation; not a physical safety system. */
@Service
public class SimulationService {

    private static final double TEMPERATURE_THRESHOLD = 50.0;
    private static final double LUMINOSITY_THRESHOLD = 140.0;
    private static final double RELIABILITY_THRESHOLD = 0.9;

    private final LocationRepository locationRepository;
    private final SensorRepository sensorRepository;
    private final MeasurementRepository measurementRepository;
    private final RandomGenerator random;
    private final Clock clock;
    private final FireAlertService fireAlertService;

    @Autowired
    public SimulationService(LocationRepository locationRepository,
                             SensorRepository sensorRepository,
                             MeasurementRepository measurementRepository,
                             FireAlertService fireAlertService) {
        this(locationRepository, sensorRepository, measurementRepository,
                RandomGenerator.getDefault(), Clock.systemDefaultZone(), fireAlertService);
    }

    // Retained for existing deterministic Mockito tests (no alert persistence in this constructor).
    SimulationService(LocationRepository locationRepository,
                      SensorRepository sensorRepository,
                      MeasurementRepository measurementRepository,
                      RandomGenerator random,
                      Clock clock) {
        this(locationRepository, sensorRepository, measurementRepository, random, clock, null);
    }

    SimulationService(LocationRepository locationRepository,
                      SensorRepository sensorRepository,
                      MeasurementRepository measurementRepository,
                      RandomGenerator random,
                      Clock clock,
                      FireAlertService fireAlertService) {
        this.locationRepository = Objects.requireNonNull(locationRepository);
        this.sensorRepository = Objects.requireNonNull(sensorRepository);
        this.measurementRepository = Objects.requireNonNull(measurementRepository);
        this.random = Objects.requireNonNull(random);
        this.clock = Objects.requireNonNull(clock);
        this.fireAlertService = fireAlertService;
    }

    /** Refresh both sensors for one location in a single DB transaction. */
    @Transactional
    public SimulationResult refreshLocation(Long locationId) {
        Location location = locationRepository.findById(Objects.requireNonNull(locationId))
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + locationId));
        return refresh(location);
    }

    /** Refresh every location in a building; all changes commit or roll back together. */
    @Transactional
    public List<SimulationResult> refreshBuilding(Long buildingId) {
        Objects.requireNonNull(buildingId, "Building ID is required");
        List<Location> locations = locationRepository
                .findByFloorBuildingIdOrderByFloorLevelNumberAscCodeAsc(buildingId);
        if (locations.isEmpty()) {
            throw new IllegalArgumentException("No locations found in building: " + buildingId);
        }
        List<SimulationResult> results = new ArrayList<>(locations.size());
        for (Location location : locations) {
            results.add(refresh(location));
        }
        return List.copyOf(results);
    }

    private SimulationResult refresh(Location location) {
        List<Sensor> sensors = sensorRepository.findByLocationIdOrderByTypeAsc(location.getId());
        Map<SensorType, Sensor> byType = new EnumMap<>(SensorType.class);
        for (Sensor sensor : sensors) {
            if (byType.putIfAbsent(sensor.getType(), sensor) != null) {
                throw new IllegalStateException("Duplicate sensor type for location " + location.getId());
            }
        }
        if (sensors.size() != 2 || !byType.containsKey(SensorType.TEMPERATURE)
                || !byType.containsKey(SensorType.LUMINOSITY)) {
            throw new IllegalStateException("Location " + location.getId()
                    + " must have one temperature and one luminosity sensor.");
        }

        Sensor temperatureSensor = byType.get(SensorType.TEMPERATURE);
        Sensor luminositySensor = byType.get(SensorType.LUMINOSITY);
        if (temperatureSensor.getRefreshCount() != luminositySensor.getRefreshCount()) {
            throw new IllegalStateException("Sensor simulation steps are not synchronized in location "
                    + location.getId());
        }

        temperatureSensor.advanceSimulationStep();
        luminositySensor.advanceSimulationStep();
        LocalDateTime measuredAt = LocalDateTime.now(clock);

        double temperature = MeasurementGenerator.generate(SensorType.TEMPERATURE, random);
        double luminosity = MeasurementGenerator.generate(SensorType.LUMINOSITY, random);
        Measurement temperatureMeasurement = new Measurement(temperatureSensor, temperature, measuredAt);
        Measurement luminosityMeasurement = new Measurement(luminositySensor, luminosity, measuredAt);

        measurementRepository.saveAll(List.of(temperatureMeasurement, luminosityMeasurement));
        sensorRepository.saveAll(List.of(temperatureSensor, luminositySensor));

        double combined = MeasurementGenerator.combinedReliability(
                temperatureMeasurement.getReliabilitySnapshot(),
                luminosityMeasurement.getReliabilitySnapshot());
        boolean fireDetected = temperature > TEMPERATURE_THRESHOLD
                && luminosity > LUMINOSITY_THRESHOLD
                && combined > RELIABILITY_THRESHOLD;

        SimulationResult result = new SimulationResult(location.getId(), temperatureMeasurement.getSimulationStep(),
                temperature, luminosity,
                temperatureMeasurement.getReliabilitySnapshot(),
                luminosityMeasurement.getReliabilitySnapshot(), combined, fireDetected, measuredAt);
        if (fireAlertService != null) {
            fireAlertService.process(location, result);
        }
        return result;
    }
}

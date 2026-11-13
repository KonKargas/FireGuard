package com.kkargas.fireguard.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.kkargas.fireguard.enums.SensorType;

class MeasurementTest {

    private static final LocalDateTime TIME = LocalDateTime.of(2026, 10, 30, 12, 0);

    private Sensor sensor(SensorType type) {
        // Location persistence is not needed for these isolated unit tests.
        Location location = new Location();
        return new Sensor("TEST-" + type, type, location, 1.0, 2.0, 1.5);
    }

    @Test
    void temperatureMustBeIntegerAndBetweenZeroAndHundred() {
        Sensor sensor = sensor(SensorType.TEMPERATURE);
        assertEquals(0.0, new Measurement(sensor, 0, TIME).getValue());
        assertEquals(100.0, new Measurement(sensor, 100, TIME).getValue());
        assertThrows(IllegalArgumentException.class, () -> new Measurement(sensor, 50.5, TIME));
        assertThrows(IllegalArgumentException.class, () -> new Measurement(sensor, 101, TIME));
        assertThrows(IllegalArgumentException.class, () -> new Measurement(sensor, -1, TIME));
    }

    @Test
    void luminositySupportsDecimalsFromZeroToTwoHundred() {
        Sensor sensor = sensor(SensorType.LUMINOSITY);
        assertEquals(140.25, new Measurement(sensor, 140.25, TIME).getValue());
        assertEquals(200.0, new Measurement(sensor, 200, TIME).getValue());
        assertThrows(IllegalArgumentException.class, () -> new Measurement(sensor, 200.01, TIME));
        assertThrows(IllegalArgumentException.class, () -> new Measurement(sensor, Double.NaN, TIME));
    }

    @Test
    void snapshotReliabilityDoesNotChangeWhenSensorAdvances() {
        Sensor sensor = sensor(SensorType.TEMPERATURE);
        sensor.advanceSimulationStep();
        Measurement measurement = new Measurement(sensor, 65, TIME);

        assertEquals(1L, measurement.getSimulationStep());
        assertEquals(Math.exp(-0.001), measurement.getReliabilitySnapshot(), 1e-12);
        sensor.advanceSimulationStep();
        assertEquals(Math.exp(-0.001), measurement.getReliabilitySnapshot(), 1e-12);
        assertEquals(2L, sensor.getRefreshCount());
        assertEquals(TIME, measurement.getMeasuredAt());
    }

    @Test
    void reliabilityDecreasesAccordingToOriginalFormula() {
        Sensor sensor = sensor(SensorType.LUMINOSITY);
        assertEquals(1.0, sensor.getReliability(), 1e-12);
        for (int i = 0; i < 100; i++) sensor.advanceSimulationStep();
        assertEquals(Math.exp(-0.1), sensor.getReliability(), 1e-12);
    }
}

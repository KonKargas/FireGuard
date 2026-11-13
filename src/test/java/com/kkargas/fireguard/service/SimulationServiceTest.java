package com.kkargas.fireguard.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.kkargas.fireguard.dto.SimulationResult;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.entity.Measurement;
import com.kkargas.fireguard.entity.Sensor;
import com.kkargas.fireguard.enums.SensorType;
import com.kkargas.fireguard.repository.LocationRepository;
import com.kkargas.fireguard.repository.MeasurementRepository;
import com.kkargas.fireguard.repository.SensorRepository;

class SimulationServiceTest {

    private LocationRepository locations;
    private SensorRepository sensors;
    private MeasurementRepository measurements;
    private SimulationService service;
    private Location location;
    private Sensor temperature;
    private Sensor luminosity;
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 10, 30, 12, 0);

    @BeforeEach
    void setUp() {
        locations = mock(LocationRepository.class);
        sensors = mock(SensorRepository.class);
        measurements = mock(MeasurementRepository.class);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC);
        service = new SimulationService(locations, sensors, measurements, new Random(42), fixedClock);
        location = new Location();
        ReflectionTestUtils.setField(location, "id", 10L);
        temperature = new Sensor("T-101", SensorType.TEMPERATURE, location, 1, 1, 2);
        luminosity = new Sensor("L-101", SensorType.LUMINOSITY, location, 1, 1, 2);
        when(locations.findById(10L)).thenReturn(Optional.of(location));
        when(sensors.findByLocationIdOrderByTypeAsc(any())).thenReturn(List.of(temperature, luminosity));
    }

    @Test
    void refreshCreatesTwoMeasurementsAtSameStepAndTime() {
        SimulationResult result = service.refreshLocation(10L);
        assertEquals(1L, result.simulationStep());
        assertEquals(1L, temperature.getRefreshCount());
        assertEquals(1L, luminosity.getRefreshCount());
        assertEquals(FIXED_TIME, result.measuredAt());
        assertEquals(Math.exp(-0.002), result.combinedReliability(), 1e-12);
        assertTrue(result.temperature() >= 0 && result.temperature() <= 100);
        assertTrue(result.luminosity() >= 0 && result.luminosity() <= 200);
        verify(measurements).saveAll(argThat(entries -> {
            int count = 0;
            for (Measurement m : entries) {
                if (m.getSimulationStep() != 1L || !FIXED_TIME.equals(m.getMeasuredAt())) return false;
                count++;
            }
            return count == 2;
        }));
    }

    @Test
    void secondRefreshIncrementsBothSteps() {
        service.refreshLocation(10L);
        SimulationResult result = service.refreshLocation(10L);
        assertEquals(2L, result.simulationStep());
        assertEquals(Math.exp(-0.004), result.combinedReliability(), 1e-12);
        verify(measurements, times(2)).saveAll(any());
    }

    @Test
    void incompleteLocationIsRejectedWithoutPersisting() {
        when(sensors.findByLocationIdOrderByTypeAsc(any())).thenReturn(List.of(temperature));
        assertThrows(IllegalStateException.class, () -> service.refreshLocation(10L));
        assertEquals(0L, temperature.getRefreshCount());
        verifyNoInteractions(measurements);
    }

    @Test
    void mismatchedStepsAreRejected() {
        luminosity.advanceSimulationStep();
        assertThrows(IllegalStateException.class, () -> service.refreshLocation(10L));
        verifyNoInteractions(measurements);
    }

    @Test
    void refreshBuildingProcessesEveryLocation() {
        when(locations.findByFloorBuildingIdOrderByFloorLevelNumberAscCodeAsc(20L))
                .thenReturn(List.of(location));
        List<SimulationResult> result = service.refreshBuilding(20L);
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().simulationStep());
    }
}

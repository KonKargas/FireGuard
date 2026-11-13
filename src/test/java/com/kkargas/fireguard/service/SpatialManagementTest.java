package com.kkargas.fireguard.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.kkargas.fireguard.dto.LocationGeometry;
import com.kkargas.fireguard.entity.Building;
import com.kkargas.fireguard.entity.Floor;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.enums.BuildingType;
import com.kkargas.fireguard.repository.BuildingRepository;
import com.kkargas.fireguard.repository.FloorRepository;
import com.kkargas.fireguard.repository.FireAlertRepository;
import com.kkargas.fireguard.repository.LocationRepository;
import com.kkargas.fireguard.repository.MeasurementRepository;
import com.kkargas.fireguard.repository.SensorRepository;

class SpatialManagementTest {
    @Test
    void adjacentRoomsDoNotOverlap() {
        var a = new LocationGeometry(0, 0, 0, 4, 5, 3);
        var b = new LocationGeometry(4, 0, 0, 4, 5, 3);
        assertFalse(a.overlaps(b));
    }

    @Test
    void overlappingRoomsAreDetected() {
        var a = new LocationGeometry(0, 0, 0, 4, 5, 3);
        var b = new LocationGeometry(3, 1, 0, 4, 5, 3);
        assertTrue(a.overlaps(b));
    }

    @Test
    void invalidRoomDimensionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new LocationGeometry(0, 0, 0, -1, 5, 3));
        assertThrows(IllegalArgumentException.class,
                () -> new LocationGeometry(Double.NaN, 0, 0, 4, 5, 3));
    }

    @Test
    void sensorCoordinatesMustBeInsideLocalRoomDimensions() {
        var room = new LocationGeometry(20, 30, 0, 4, 5, 3);
        assertTrue(SensorService.isInside(room, 2, 2, 2));
        assertFalse(SensorService.isInside(room, 5, 2, 2));
        assertFalse(SensorService.isInside(room, 2, 2, Double.NaN));
    }

    @Test
    void duplicateFloorIsRejected() {
        var buildingsRepo = mock(BuildingRepository.class);
        var floorsRepo = mock(FloorRepository.class);
        var buildingService = new BuildingService(buildingsRepo);
        var floorService = new FloorService(floorsRepo, buildingService);
        var building = new Building("Demo", BuildingType.HOTEL, null, null);
        when(buildingsRepo.findById(1L)).thenReturn(java.util.Optional.of(building));
        when(floorsRepo.existsByBuildingIdAndLevelNumber(1L, 1)).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> floorService.create(1L, 1, "First", 3.0, null));
        verify(floorsRepo, never()).save(any());
    }

    @Test
    void roomOverlappingExistingRoomIsRejected() {
        var building = new Building("Demo", BuildingType.HOTEL, null, null);
        var floor = new Floor(0, "Ground", 0, null, building);
        var floorsRepo = mock(FloorRepository.class);
        var buildingsRepo = mock(BuildingRepository.class);
        var locationsRepo = mock(LocationRepository.class);
        var sensorsRepo = mock(SensorRepository.class);
        var floorService = new FloorService(floorsRepo, new BuildingService(buildingsRepo));
        var service = new LocationService(locationsRepo, floorService, sensorsRepo,
                mock(MeasurementRepository.class), mock(FireAlertRepository.class));
        when(floorsRepo.findById(1L)).thenReturn(java.util.Optional.of(floor));
        var existing = new Location("101", "Room A", null, floor, 0, 0, 0, 4, 5, 3);
        when(locationsRepo.findByFloorIdOrderByCodeAsc(1L)).thenReturn(List.of(existing));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(1L, "102", "Room B", null,
                        new LocationGeometry(2, 2, 0, 4, 5, 3)));
        verify(locationsRepo, never()).save(any());
    }

    @Test
    void sensorPairRequiresEmptyLocation() {
        var sensorsRepo = mock(SensorRepository.class);
        var locationsService = mock(LocationService.class);
        var measurementsRepo = mock(MeasurementRepository.class);
        var service = new SensorService(sensorsRepo, locationsService, measurementsRepo);
        var location = new Location();
        when(locationsService.get(5L)).thenReturn(location);
        when(sensorsRepo.countByLocationId(5L)).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> service.installDefaultPair(5L));
        verify(sensorsRepo, never()).saveAll(any());
    }
}

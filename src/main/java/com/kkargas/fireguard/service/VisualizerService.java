package com.kkargas.fireguard.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kkargas.fireguard.dto.Building3DDto;
import com.kkargas.fireguard.dto.Building3DDto.Floor3D;
import com.kkargas.fireguard.dto.Building3DDto.Room3D;
import com.kkargas.fireguard.dto.Building3DDto.Sensor3D;
import com.kkargas.fireguard.entity.Building;
import com.kkargas.fireguard.entity.Floor;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.entity.Sensor;
import com.kkargas.fireguard.enums.AlertStatus;
import com.kkargas.fireguard.repository.BuildingRepository;
import com.kkargas.fireguard.repository.FireAlertRepository;
import com.kkargas.fireguard.repository.LocationRepository;
import com.kkargas.fireguard.repository.SensorRepository;

@Service
@Transactional(readOnly = true)
public class VisualizerService {
    private final BuildingRepository buildings;
    private final FloorService floors;
    private final LocationRepository locations;
    private final SensorRepository sensors;
    private final FireAlertRepository alerts;

    public VisualizerService(BuildingRepository buildings, FloorService floors,
                             LocationRepository locations, SensorRepository sensors,
                             FireAlertRepository alerts) {
        this.buildings = buildings;
        this.floors = floors;
        this.locations = locations;
        this.sensors = sensors;
        this.alerts = alerts;
    }

    public Building3DDto building(Long buildingId) {
        Building building = buildings.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Building not found: " + buildingId));

        Set<Long> activeRoomIds = new HashSet<>();
        alerts.findByStatusOrderByDetectedAtDesc(AlertStatus.ACTIVE).forEach(alert -> {
            // location association is accessed inside the read-only transaction
            activeRoomIds.add(alert.getLocation().getId());
        });

        List<Floor3D> floorData = new ArrayList<>();
        for (Floor floor : floors.findByBuilding(buildingId)) {
            List<Room3D> roomData = new ArrayList<>();
            for (Location room : locations.findByFloorIdOrderByCodeAsc(floor.getId())) {
                List<Sensor3D> sensorData = new ArrayList<>();
                for (Sensor sensor : sensors.findByLocationIdOrderByTypeAsc(room.getId())) {
                    sensorData.add(new Sensor3D(sensor.getId(), sensor.getCode(),
                        sensor.getType().name(), sensor.getPositionX(), sensor.getPositionY(),
                        sensor.getPositionZ(), sensor.getReliability(), sensor.getRefreshCount()));
                }
                roomData.add(new Room3D(room.getId(), room.getCode(), room.getName(), room.getType().name(),
                    room.getPositionX(), room.getPositionY(), room.getPositionZ(),
                    room.getWidthMeters(), room.getDepthMeters(), room.getHeightMeters(),
                    activeRoomIds.contains(room.getId()), sensorData));
            }
            floorData.add(new Floor3D(floor.getId(), floor.getLevelNumber(),
                floor.getName(), floor.getElevationMeters(), roomData));
        }
        return new Building3DDto(building.getId(), building.getName(), floorData);
    }
}

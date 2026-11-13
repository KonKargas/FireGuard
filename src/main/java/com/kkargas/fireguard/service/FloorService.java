package com.kkargas.fireguard.service;

import com.kkargas.fireguard.util.TextFields;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kkargas.fireguard.entity.Building;
import com.kkargas.fireguard.entity.Floor;
import com.kkargas.fireguard.repository.FloorRepository;

@Service
@Transactional(readOnly = true)
public class FloorService {
    private final FloorRepository floors;
    private final BuildingService buildings;

    public FloorService(FloorRepository floors, BuildingService buildings) {
        this.floors = floors;
        this.buildings = buildings;
    }

    public Floor get(Long id) {
        return floors.findById(Objects.requireNonNull(id, "Floor ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Floor not found: " + id));
    }

    public Floor getDetails(Long id) {
        return floors.findDetailsById(Objects.requireNonNull(id, "Floor ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Floor not found: " + id));
    }

    public List<Floor> findByBuilding(Long buildingId) {
        buildings.get(buildingId);
        return floors.findByBuildingIdOrderByLevelNumberAsc(buildingId);
    }

    @Transactional
    public Floor create(Long buildingId, int level, String name, double elevation, String description) {
        Building building = buildings.get(buildingId);
        if (floors.existsByBuildingIdAndLevelNumber(buildingId, level)) {
            throw new IllegalArgumentException("Floor level already exists in building: " + level);
        }
        Floor floor = new Floor(level, TextFields.required(name, "Floor name", 100),
                elevation, TextFields.optional(description, 500), building);
        return floors.save(floor);
    }

    @Transactional
    public Floor update(Long id, int level, String name, double elevation, String description) {
        Floor floor = get(id);
        if (floor.getLevelNumber() != level
                && floors.existsByBuildingIdAndLevelNumber(floor.getBuilding().getId(), level)) {
            throw new IllegalArgumentException("Floor level already exists in building: " + level);
        }
        floor.setLevelNumber(level);
        floor.setName(TextFields.required(name, "Floor name", 100));
        floor.setElevationMeters(elevation);
        floor.setDescription(TextFields.optional(description, 500));
        return floor;
    }

    @Transactional
    public void deleteEmpty(Long id) {
        Floor floor = get(id);
        if (!floor.getLocations().isEmpty()) {
            throw new IllegalStateException("Remove locations before deleting floor");
        }
        floors.delete(floor);
    }
}

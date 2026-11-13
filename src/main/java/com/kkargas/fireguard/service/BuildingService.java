package com.kkargas.fireguard.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kkargas.fireguard.entity.Building;
import com.kkargas.fireguard.enums.BuildingType;
import com.kkargas.fireguard.repository.BuildingRepository;
import com.kkargas.fireguard.util.TextFields;

@Service
@Transactional(readOnly = true)
public class BuildingService {
    private final BuildingRepository buildings;

    public BuildingService(BuildingRepository buildings) {
        this.buildings = buildings;
    }

    public List<Building> findAll() {
        return buildings.findAllByOrderByNameAsc();
    }

    public Building get(Long id) {
        return buildings.findById(Objects.requireNonNull(id, "Building ID is required"))
                .orElseThrow(() -> new IllegalArgumentException("Building not found: " + id));
    }

    @Transactional
    public Building create(String name, BuildingType type, String address, String description) {
        String normalizedName = TextFields.required(name, "Building name", 100);
        Objects.requireNonNull(type, "Building type is required");
        if (buildings.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Building name already exists: " + normalizedName);
        }
        return buildings.save(new Building(normalizedName, type, TextFields.optional(address, 255), TextFields.optional(description, 1000)));
    }

    @Transactional
    public Building update(Long id, String name, BuildingType type, String address, String description) {
        Building building = get(id);
        String normalizedName = TextFields.required(name, "Building name", 100);
        Objects.requireNonNull(type, "Building type is required");
        if (!building.getName().equalsIgnoreCase(normalizedName)
                && buildings.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Building name already exists: " + normalizedName);
        }
        building.setName(normalizedName);
        building.setType(type);
        building.setAddress(TextFields.optional(address, 255));
        building.setDescription(TextFields.optional(description, 1000));
        return building;
    }

    // Deleting a building with floors would cascade to locations. Block it by default.
    @Transactional
    public void deleteEmpty(Long id) {
        Building building = get(id);
        if (!building.getFloors().isEmpty()) {
            throw new IllegalStateException("Remove floors before deleting this building");
        }
        buildings.delete(building);
    }

}

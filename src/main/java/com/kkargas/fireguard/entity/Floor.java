package com.kkargas.fireguard.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.*;

@Entity
@Table(name = "floors", uniqueConstraints = @UniqueConstraint(
    name = "uk_floor_building_level", columnNames = {"building_id", "level_number"}))
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -1 = basement, 0 = ground floor, 1 = first floor, etc.
    @Column(name = "level_number", nullable = false)
    private int levelNumber;

    @Column(nullable = false, length = 100)
    private String name;

    // Height of this floor's local origin relative to building origin, in meters.
    @Column(name = "elevation_meters", nullable = false)
    private double elevationMeters;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_floor_building"))
    private Building building;

    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Location> locations = new ArrayList<>();

    public Floor() {}

    public Floor(int levelNumber, String name, double elevationMeters,
                 String description, Building building) {
        this.levelNumber = levelNumber;
        this.name = name;
        setElevationMeters(elevationMeters);
        this.description = description;
        this.building = building;
    }

    public Floor(Floor other) {
        this(other.levelNumber, other.name, other.elevationMeters, other.description, other.building);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public int getLevelNumber() { return levelNumber; }
    public void setLevelNumber(int levelNumber) { this.levelNumber = levelNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getElevationMeters() { return elevationMeters; }
    public void setElevationMeters(double elevationMeters) {
        if (!Double.isFinite(elevationMeters))
            throw new IllegalArgumentException("Elevation must be finite");
        this.elevationMeters = elevationMeters;
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Building getBuilding() { return building; }
    public void setBuilding(Building building) { this.building = building; }
    public List<Location> getLocations() { return locations; }

    public void addLocation(Location location) {
        Objects.requireNonNull(location, "Location cannot be null");
        if (location.getFloor() != null && location.getFloor() != this)
            throw new IllegalArgumentException("Location belongs to another floor");
        if (!locations.contains(location)) locations.add(location);
        location.setFloor(this);
    }

    public void removeLocation(Location location) {
        if (locations.remove(location)) location.setFloor(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Floor other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Floor{id=" + id + ", levelNumber=" + levelNumber + ", name='" + name + "'}";
    }
}

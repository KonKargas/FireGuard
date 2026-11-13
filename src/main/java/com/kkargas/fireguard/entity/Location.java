package com.kkargas.fireguard.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.*;
import com.kkargas.fireguard.enums.LocationType;

@Entity
@Table(name = "locations", uniqueConstraints = @UniqueConstraint(
    name = "uk_location_floor_code", columnNames = {"floor_id", "code"}))
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // Local floor coordinates, in meters; origin = lower-left floor corner.
    // For the eventual 3D renderer, coordinates describe the room's corner.
    @Column(name = "position_x", nullable = false)
    private double positionX;

    @Column(name = "position_y", nullable = false)
    private double positionY;

    @Column(name = "position_z", nullable = false)
    private double positionZ;

    @Column(name = "width_meters", nullable = false)
    private double widthMeters;

    @Column(name = "depth_meters", nullable = false)
    private double depthMeters;

    @Column(name = "height_meters", nullable = false)
    private double heightMeters;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_location_floor"))
    private Floor floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 30)
    private LocationType type = LocationType.HOTEL_ROOM;

    public Location() {}

    public Location(String code, String name, String description, Floor floor,
                    double positionX, double positionY, double positionZ,
                    double widthMeters, double depthMeters, double heightMeters) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.floor = floor;
        setPosition(positionX, positionY, positionZ);
        setDimensions(widthMeters, depthMeters, heightMeters);
    }

    public Location(Location other) {
        this(other.code, other.name, other.description, other.floor,
             other.positionX, other.positionY, other.positionZ,
             other.widthMeters, other.depthMeters, other.heightMeters);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void setPosition(double x, double y, double z) {
        requireFinite(x, "X");
        requireFinite(y, "Y");
        requireFinite(z, "Z");
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
    }

    public void setDimensions(double width, double depth, double height) {
        requirePositive(width, "Width");
        requirePositive(depth, "Depth");
        requirePositive(height, "Height");
        this.widthMeters = width;
        this.depthMeters = depth;
        this.heightMeters = height;
    }

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value))
            throw new IllegalArgumentException(field + " must be finite");
    }

    private static void requirePositive(double value, String field) {
        if (!Double.isFinite(value) || value <= 0)
            throw new IllegalArgumentException(field + " must be positive and finite");
    }

    public Long getId() { return id; }
    public LocationType getType() { return type == null ? LocationType.HOTEL_ROOM : type; }
    public void setType(LocationType type) { this.type = java.util.Objects.requireNonNull(type); }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Floor getFloor() { return floor; }
    public void setFloor(Floor floor) { this.floor = floor; }
    public double getPositionX() { return positionX; }
    public double getPositionY() { return positionY; }
    public double getPositionZ() { return positionZ; }
    public void setPositionX(double x) { requireFinite(x, "X"); this.positionX = x; }
    public void setPositionY(double y) { requireFinite(y, "Y"); this.positionY = y; }
    public void setPositionZ(double z) { requireFinite(z, "Z"); this.positionZ = z; }
    public double getWidthMeters() { return widthMeters; }
    public double getDepthMeters() { return depthMeters; }
    public double getHeightMeters() { return heightMeters; }
    public void setWidthMeters(double width) { requirePositive(width, "Width"); this.widthMeters = width; }
    public void setDepthMeters(double depth) { requirePositive(depth, "Depth"); this.depthMeters = depth; }
    public void setHeightMeters(double height) { requirePositive(height, "Height"); this.heightMeters = height; }

    // Vertical coordinate in the building's reference frame.
    public double getBuildingZ() {
        return (floor == null ? 0 : floor.getElevationMeters()) + positionZ;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Location other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Location{id=" + id + ", code='" + code + "', name='" + name +
               "', x=" + positionX + ", y=" + positionY + ", z=" + positionZ + "}";
    }
}

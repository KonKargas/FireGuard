package com.kkargas.fireguard.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.kkargas.fireguard.enums.SensorType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "sensors", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sensor_location_type", columnNames = {"location_id", "type"})
})
public class Sensor {

    private static final double DECAY_CONSTANT = 0.001;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SensorType type;

    // Coordinates are relative to the Location's origin (in meters).
    @Column(name = "position_x", nullable = false)
    private double positionX;

    @Column(name = "position_y", nullable = false)
    private double positionY;

    @Column(name = "position_z", nullable = false)
    private double positionZ;

    // Simulation time t: number of completed measurement refreshes.
    @Column(name = "refresh_count", nullable = false)
    private long refreshCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sensor_location"))
    private Location location;

    protected Sensor() {
        // Required by JPA.
    }

    public Sensor(String code, SensorType type, Location location,
                  double positionX, double positionY, double positionZ) {
        setCode(code);
        setType(type);
        setLocation(location);
        setPosition(positionX, positionY, positionZ);
    }

    // Copies configuration; does not duplicate the database identity or history.
    public Sensor(Sensor other) {
        this(other.code, other.type, other.location,
                other.positionX, other.positionY, other.positionZ);
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

    private static void requireFinite(double value, String field) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite.");
        }
    }

    // Called once for each simulation measurement refresh.
    public void advanceSimulationStep() {
        if (refreshCount == Long.MAX_VALUE) {
            throw new IllegalStateException("Simulation step limit reached.");
        }
        refreshCount++;
    }

    // Reliability is derived rather than stored, avoiding inconsistent values.
    @Transient
    public double getReliability() {
        return Math.exp(-DECAY_CONSTANT * refreshCount);
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) {
        if (code == null || code.isBlank() || code.length() > 40) {
            throw new IllegalArgumentException("Sensor code must contain 1 to 40 characters.");
        }
        this.code = code.trim();
    }
    public SensorType getType() { return type; }
    public void setType(SensorType type) { this.type = Objects.requireNonNull(type, "Sensor type is required."); }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = Objects.requireNonNull(location, "Location is required."); }
    public double getPositionX() { return positionX; }
    public double getPositionY() { return positionY; }
    public double getPositionZ() { return positionZ; }
    public void setPositionX(double x) { requireFinite(x, "X"); this.positionX = x; }
    public void setPositionY(double y) { requireFinite(y, "Y"); this.positionY = y; }
    public void setPositionZ(double z) { requireFinite(z, "Z"); this.positionZ = z; }
    public long getRefreshCount() { return refreshCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Sensor other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Sensor{id=" + id + ", code='" + code + "', type=" + type
                + ", refreshCount=" + refreshCount + "}";
    }
}

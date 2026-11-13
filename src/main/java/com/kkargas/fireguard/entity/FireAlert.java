package com.kkargas.fireguard.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.kkargas.fireguard.dto.SimulationResult;
import com.kkargas.fireguard.enums.AlertStatus;
import jakarta.persistence.*;

/** A simulated fire-detection episode; not a real emergency notification. */
@Entity
@Table(name = "fire_alerts", indexes = {
    @Index(name = "idx_fire_alert_location_status", columnList = "location_id,status"),
    @Index(name = "idx_fire_alert_detected_at", columnList = "detected_at")
})
public class FireAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_fire_alert_location"))
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "simulation_step", nullable = false, updatable = false)
    private long simulationStep;

    @Column(nullable = false, updatable = false)
    private double temperature;

    @Column(nullable = false, updatable = false)
    private double luminosity;

    @Column(name = "combined_reliability", nullable = false, updatable = false)
    private double combinedReliability;

    protected FireAlert() { }

    public FireAlert(Location location, SimulationResult result) {
        this.location = Objects.requireNonNull(location, "Location is required");
        Objects.requireNonNull(result, "Result is required");
        if (location.getId() == null || !location.getId().equals(result.locationId())) {
            throw new IllegalArgumentException("Result does not belong to location");
        }
        if (!result.fireDetected()) {
            throw new IllegalArgumentException("Cannot create alert from non-fire result");
        }
        if (result.simulationStep() < 0 || !Double.isFinite(result.temperature())
                || !Double.isFinite(result.luminosity())
                || !Double.isFinite(result.combinedReliability())) {
            throw new IllegalArgumentException("Invalid simulation result");
        }
        this.status = AlertStatus.ACTIVE;
        this.detectedAt = Objects.requireNonNull(result.measuredAt(), "Detection time is required");
        this.simulationStep = result.simulationStep();
        this.temperature = result.temperature();
        this.luminosity = result.luminosity();
        this.combinedReliability = result.combinedReliability();
    }

    public void resolve(LocalDateTime timestamp) {
        Objects.requireNonNull(timestamp, "Resolution time is required");
        if (status == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Alert is already resolved");
        }
        if (timestamp.isBefore(detectedAt)) {
            throw new IllegalArgumentException("Resolution precedes detection");
        }
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = timestamp;
    }

    public Long getId() { return id; }
    public Location getLocation() { return location; }
    public AlertStatus getStatus() { return status; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public long getSimulationStep() { return simulationStep; }
    public double getTemperature() { return temperature; }
    public double getLuminosity() { return luminosity; }
    public double getCombinedReliability() { return combinedReliability; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FireAlert other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "FireAlert{id=" + id + ", locationId=" + location.getId()
                + ", status=" + status + ", detectedAt=" + detectedAt + "}";
    }
}

package com.kkargas.fireguard.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import com.kkargas.fireguard.enums.SensorType;
import jakarta.persistence.*;

/** Historical snapshot of one sensor reading (simulation, not a physical detector). */
@Entity
@Table(name = "measurements", indexes = {
        @Index(name = "idx_measurement_sensor_time", columnList = "sensor_id,measured_at"),
        @Index(name = "idx_measurement_sensor_step", columnList = "sensor_id,simulation_step")
})
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sensor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_measurement_sensor"))
    private Sensor sensor;

    @Column(nullable = false)
    private double value;

    @Column(name = "simulation_step", nullable = false)
    private long simulationStep;

    @Column(name = "reliability_snapshot", nullable = false)
    private double reliabilitySnapshot;

    @Column(name = "measured_at", nullable = false, updatable = false)
    private LocalDateTime measuredAt;

    protected Measurement() {
        // Required by JPA.
    }

    public Measurement(Sensor sensor, double value, LocalDateTime measuredAt) {
        this.sensor = Objects.requireNonNull(sensor, "Sensor is required.");
        this.measuredAt = Objects.requireNonNull(measuredAt, "Measurement time is required.");
        validateValue(sensor.getType(), value);
        this.value = value;
        this.simulationStep = sensor.getRefreshCount();
        this.reliabilitySnapshot = sensor.getReliability();
    }

    /** Copy the reading's data, without duplicating its persistence identity. */
    public Measurement(Measurement other) {
        this(Objects.requireNonNull(other, "Measurement is required.").sensor,
                other.value, other.measuredAt);
        this.simulationStep = other.simulationStep;
        this.reliabilitySnapshot = other.reliabilitySnapshot;
    }

    private static void validateValue(SensorType type, double value) {
        Objects.requireNonNull(type, "Sensor type is required.");
        if (!Double.isFinite(value) || value < type.getMinimumValue()
                || value > type.getMaximumValue()) {
            throw new IllegalArgumentException("Measurement outside allowed range for " + type);
        }
        if (type == SensorType.TEMPERATURE && value != Math.rint(value)) {
            throw new IllegalArgumentException("Simulated temperature must be an integer.");
        }
    }

    public Long getId() { return id; }
    public Sensor getSensor() { return sensor; }
    public double getValue() { return value; }
    public long getSimulationStep() { return simulationStep; }
    public double getReliabilitySnapshot() { return reliabilitySnapshot; }
    public LocalDateTime getMeasuredAt() { return measuredAt; }
    public String getUnit() { return sensor.getType().getUnit(); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Measurement other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Measurement{id=" + id + ", value=" + value
                + ", step=" + simulationStep + ", reliability=" + reliabilitySnapshot
                + ", measuredAt=" + measuredAt + "}";
    }
}

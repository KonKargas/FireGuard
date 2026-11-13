package com.kkargas.fireguard.util;

import java.util.Objects;
import java.util.random.RandomGenerator;

import com.kkargas.fireguard.enums.SensorType;

/** Generates simulated measurements; does not update sensors or access the database. */
public final class MeasurementGenerator {

    private MeasurementGenerator() { }

    public static double generate(SensorType type, RandomGenerator random) {
        Objects.requireNonNull(type, "Sensor type is required.");
        Objects.requireNonNull(random, "Random generator is required.");

        return switch (type) {
            case TEMPERATURE -> random.nextInt(101); // inclusive 0..100
            case LUMINOSITY -> Math.round(random.nextDouble(0.0, 200.0) * 100.0) / 100.0;
        };
    }

    public static double combinedReliability(double first, double second) {
        if (!Double.isFinite(first) || !Double.isFinite(second)
                || first < 0 || first > 1 || second < 0 || second > 1) {
            throw new IllegalArgumentException("Reliabilities must be in [0, 1].");
        }
        return first * second;
    }
}

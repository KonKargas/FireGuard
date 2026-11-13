package com.kkargas.fireguard.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;
import com.kkargas.fireguard.enums.SensorType;

class MeasurementGeneratorTest {

    @Test
    void generatedTemperaturesAreIntegersInRange() {
        Random random = new Random(42);
        for (int i = 0; i < 10_000; i++) {
            double value = MeasurementGenerator.generate(SensorType.TEMPERATURE, random);
            assertTrue(value >= 0 && value <= 100);
            assertEquals(Math.rint(value), value);
        }
    }

    @Test
    void generatedLuminosityIsWithinRangeAndRoundedToTwoDecimals() {
        Random random = new Random(42);
        for (int i = 0; i < 10_000; i++) {
            double value = MeasurementGenerator.generate(SensorType.LUMINOSITY, random);
            assertTrue(value >= 0 && value <= 200);
            assertEquals(Math.rint(value * 100), value * 100, 1e-9);
        }
    }

    @Test
    void combinedReliabilityIsProductOfSensorReliabilities() {
        double each = Math.exp(-0.001 * 10);
        assertEquals(Math.exp(-0.002 * 10),
                MeasurementGenerator.combinedReliability(each, each), 1e-12);
        assertThrows(IllegalArgumentException.class,
                () -> MeasurementGenerator.combinedReliability(-0.1, 0.9));
    }
}

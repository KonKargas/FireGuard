package com.kkargas.fireguard.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocationGeometryEdgeCaseTest {

    @Test
    void roomsTouchingOnAnyAxisDoNotOverlap() {
        LocationGeometry a = new LocationGeometry(0, 0, 0, 4, 5, 3);

        assertFalse(a.overlaps(new LocationGeometry(4, 0, 0, 2, 5, 3)));
        assertFalse(a.overlaps(new LocationGeometry(0, 5, 0, 4, 2, 3)));
        assertFalse(a.overlaps(new LocationGeometry(0, 0, 3, 4, 5, 2)));
    }

    @Test
    void aSmallIntersectionOnEachAxisCountsAsOverlap() {
        LocationGeometry a = new LocationGeometry(0, 0, 0, 4, 5, 3);

        assertTrue(a.overlaps(new LocationGeometry(3.999, 0, 0, 2, 5, 3)));
        assertTrue(a.overlaps(new LocationGeometry(0, 4.999, 0, 4, 2, 3)));
        assertTrue(a.overlaps(new LocationGeometry(0, 0, 2.999, 4, 5, 2)));
    }

    @Test
    void invalidGeometryRejectsNonFiniteAndNonPositiveDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> new LocationGeometry(Double.POSITIVE_INFINITY, 0, 0, 1, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new LocationGeometry(0, 0, 0, 0, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new LocationGeometry(0, 0, 0, 1, Double.NaN, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new LocationGeometry(0, 0, 0, 1, 1, Double.NEGATIVE_INFINITY));
    }
}

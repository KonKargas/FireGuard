package com.kkargas.fireguard.dto;

/** Axis-aligned rectangular room, coordinates and sizes expressed in meters. */
public record LocationGeometry(double x, double y, double z,
                               double width, double depth, double height) {
    public LocationGeometry {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Coordinates must be finite");
        if (!Double.isFinite(width) || !Double.isFinite(depth) || !Double.isFinite(height)
                || width <= 0 || depth <= 0 || height <= 0)
            throw new IllegalArgumentException("Dimensions must be positive finite numbers");
    }

    public boolean overlaps(LocationGeometry other) {
        return x < other.x + other.width && x + width > other.x
                && y < other.y + other.depth && y + depth > other.y
                && z < other.z + other.height && z + height > other.z;
    }
}

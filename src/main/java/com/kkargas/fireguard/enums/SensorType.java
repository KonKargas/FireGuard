package com.kkargas.fireguard.enums;

public enum SensorType {

    TEMPERATURE("Temperature", "°C", 0.0, 100.0),
    LUMINOSITY("Luminosity", "a.u.", 0.0, 200.0);

    private final String displayName;
    private final String unit;
    private final double minimumValue;
    private final double maximumValue;

    SensorType(String displayName, String unit, double minimumValue, double maximumValue) {
        this.displayName = displayName;
        this.unit = unit;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
    }

    public String getDisplayName() { return displayName; }
    public String getUnit() { return unit; }
    public double getMinimumValue() { return minimumValue; }
    public double getMaximumValue() { return maximumValue; }
}

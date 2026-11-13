package com.kkargas.fireguard.enums;

public enum BuildingType {

    HOTEL("Hotel"),
    VILLA("Villa"),
    APARTMENT("Apartment"),
    COTTAGE("Cottage"),
    OFFICE("Office"),
    RESIDENTIAL("Residential Building"),
    COMMERCIAL("Commercial Building"),
    OTHER("Other");

    private final String displayName;

    BuildingType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

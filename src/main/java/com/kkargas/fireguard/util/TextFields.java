package com.kkargas.fireguard.util;

/** Shared text-field validation rules for building, floor and location services. */
public final class TextFields {
    private TextFields() { }

    public static String required(String value, String field, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new IllegalArgumentException(field + " must have 1 to " + max + " characters");
        }
        return value.trim();
    }

    public static String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().length() > max) {
            throw new IllegalArgumentException("Text exceeds " + max + " characters");
        }
        return value.trim();
    }
}

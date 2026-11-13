package com.kkargas.fireguard.dto;

import java.time.LocalDateTime;

/** A single completed refresh of one Location. */
public record SimulationResult(
        Long locationId,
        long simulationStep,
        double temperature,
        double luminosity,
        double temperatureReliability,
        double luminosityReliability,
        double combinedReliability,
        boolean fireDetected,
        LocalDateTime measuredAt) {
}

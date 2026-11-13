package com.kkargas.fireguard.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kkargas.fireguard.dto.SimulationResult;
import com.kkargas.fireguard.entity.FireAlert;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.enums.AlertStatus;
import com.kkargas.fireguard.repository.FireAlertRepository;

/** Manages simulated detection episodes. Invoked inside SimulationService's transaction. */
@Service
public class FireAlertService {

    private final FireAlertRepository alerts;

    public FireAlertService(FireAlertRepository alerts) {
        this.alerts = Objects.requireNonNull(alerts);
    }

    @Transactional
    public void process(Location location, SimulationResult result) {
        Objects.requireNonNull(location, "Location is required");
        Objects.requireNonNull(result, "Result is required");
        if (location.getId() == null || !location.getId().equals(result.locationId())) {
            throw new IllegalArgumentException("Result location mismatch");
        }
        Optional<FireAlert> active = alerts
                .findFirstByLocationIdAndStatusOrderByDetectedAtDesc(location.getId(), AlertStatus.ACTIVE);
        if (result.fireDetected()) {
            if (active.isEmpty()) {
                alerts.save(new FireAlert(location, result));
            }
        } else {
            active.ifPresent(alert -> {
                alert.resolve(result.measuredAt());
                alerts.save(alert);
            });
        }
    }
}

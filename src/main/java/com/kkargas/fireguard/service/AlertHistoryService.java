package com.kkargas.fireguard.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kkargas.fireguard.entity.Building;
import com.kkargas.fireguard.entity.FireAlert;
import com.kkargas.fireguard.entity.Floor;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.enums.AlertStatus;
import com.kkargas.fireguard.repository.FireAlertRepository;

@Service
@Transactional(readOnly = true)
public class AlertHistoryService {
    private final FireAlertRepository repository;

    public AlertHistoryService(FireAlertRepository repository) {
        this.repository = repository;
    }

    public List<AlertRow> findAlerts(String status, Long buildingId) {
        AlertStatus filter = parseStatus(status);
        return repository.findAllByOrderByDetectedAtDesc().stream()
                .filter(alert -> filter == null || alert.getStatus() == filter)
                .filter(alert -> buildingId == null || buildingId.equals(
                        alert.getLocation().getFloor().getBuilding().getId()))
                .map(this::toRow).toList();
    }

    public AlertRow getDetails(Long id) {
        Objects.requireNonNull(id, "Alert id is required");
        return repository.findDetailsById(id).map(this::toRow)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + id));
    }

    public long countActive() { return repository.countByStatus(AlertStatus.ACTIVE); }
    public long countResolved() { return repository.countByStatus(AlertStatus.RESOLVED); }
    public long countAll() { return repository.count(); }

    private AlertStatus parseStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) return null;
        try { return AlertStatus.valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Unknown alert status: " + value); }
    }

    private AlertRow toRow(FireAlert alert) {
        Location loc = alert.getLocation();
        Floor floor = loc.getFloor();
        Building building = floor.getBuilding();
        return new AlertRow(alert.getId(), alert.getStatus(), alert.getDetectedAt(),
                alert.getResolvedAt(), alert.getSimulationStep(), alert.getTemperature(),
                alert.getLuminosity(), alert.getCombinedReliability(), loc.getId(),
                loc.getCode(), loc.getName(), floor.getId(), floor.getName(),
                building.getId(), building.getName());
    }

    public record AlertRow(Long id, AlertStatus status, LocalDateTime detectedAt,
            LocalDateTime resolvedAt, long simulationStep, double temperature,
            double luminosity, double combinedReliability, Long locationId,
            String locationCode, String locationName, Long floorId, String floorName,
            Long buildingId, String buildingName) {}
}

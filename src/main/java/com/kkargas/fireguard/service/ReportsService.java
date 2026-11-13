package com.kkargas.fireguard.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kkargas.fireguard.entity.Measurement;
import com.kkargas.fireguard.enums.AlertStatus;
import com.kkargas.fireguard.enums.SensorType;
import com.kkargas.fireguard.repository.BuildingRepository;
import com.kkargas.fireguard.repository.FireAlertRepository;
import com.kkargas.fireguard.repository.FloorRepository;
import com.kkargas.fireguard.repository.LocationRepository;
import com.kkargas.fireguard.repository.MeasurementRepository;
import com.kkargas.fireguard.repository.SensorRepository;

@Service
@Transactional(readOnly = true)
public class ReportsService {
    private final BuildingRepository buildings;
    private final MeasurementRepository measurements;
    private final FireAlertRepository alerts;
    public ReportsService(BuildingRepository buildings, FloorRepository floors,
                          LocationRepository locations, SensorRepository sensors,
                          MeasurementRepository measurements, FireAlertRepository alerts) {
        this.buildings=buildings;  
        this.measurements=measurements; this.alerts=alerts;
    }
    public Report snapshot(Long buildingId, int limit) {
        if (buildingId != null && !buildings.existsById(buildingId))
            throw new IllegalArgumentException("Building not found: " + buildingId);
        int safeLimit=Math.max(1,Math.min(5000,limit));
        long totalMeasurements=buildingId==null ? measurements.count() : measurements.countBySensorLocationFloorBuildingId(buildingId);
        long totalAlerts=buildingId==null ? alerts.count() : alerts.countByLocationFloorBuildingId(buildingId);
        long activeAlerts=buildingId==null ? alerts.countByStatus(AlertStatus.ACTIVE) : alerts.countByLocationFloorBuildingIdAndStatus(buildingId,AlertStatus.ACTIVE);
        long resolvedAlerts=buildingId==null ? alerts.countByStatus(AlertStatus.RESOLVED) : alerts.countByLocationFloorBuildingIdAndStatus(buildingId,AlertStatus.RESOLVED);
        var page=PageRequest.of(0,safeLimit);
        var result=buildingId==null ? measurements.findAllByOrderByMeasuredAtDescIdDesc(page)
                : measurements.findBySensorLocationFloorBuildingIdOrderByMeasuredAtDescIdDesc(buildingId,page);
        List<Reading> readings=new ArrayList<>();
        for (Measurement m:result.getContent()) {
            var sensor=m.getSensor(); var location=sensor.getLocation(); var floor=location.getFloor(); var building=floor.getBuilding();
            readings.add(new Reading(m.getId(),m.getMeasuredAt(),m.getSimulationStep(),m.getValue(),
                    m.getReliabilitySnapshot(),sensor.getId(),sensor.getCode(),sensor.getType().name(),
                    location.getId(),location.getCode(),floor.getName(),building.getId(),building.getName()));
        }
        return new Report(buildingId,totalMeasurements,totalAlerts,activeAlerts,resolvedAlerts,
                summary(SensorType.TEMPERATURE,buildingId),summary(SensorType.LUMINOSITY,buildingId),
                List.copyOf(readings));
    }
    private Metric summary(SensorType type,Long buildingId) {
        Object[] values= buildingId==null ? measurements.aggregateAll(type) : measurements.aggregateBuilding(type,buildingId);
        // Spring Data JPA returns the aggregate columns as an Object[] tuple.
        if (values.length==1 && values[0] instanceof Object[] nested) values=nested;
        return new Metric(type.name(), asLong(values[0]), asDouble(values[1]),asDouble(values[2]),asDouble(values[3]));
    }
    private static long asLong(Object n) {return n instanceof Number x ? x.longValue() : 0L;}
    private static Double asDouble(Object n) {return n instanceof Number x ? x.doubleValue() : null;}
    public record Metric(String type,long count,Double average,Double minimum,Double maximum) {}
    public record Reading(Long id,LocalDateTime measuredAt,long simulationStep,double value,double reliability,
                          Long sensorId,String sensorCode,String sensorType,Long locationId,String locationCode,
                          String floorName,Long buildingId,String buildingName) {}
    public record Report(Long buildingId,long measurements,long alerts,long activeAlerts,long resolvedAlerts,
                         Metric temperature,Metric luminosity,List<Reading> recentReadings) {}
}

package com.kkargas.fireguard.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.kkargas.fireguard.entity.Measurement;
import com.kkargas.fireguard.enums.SensorType;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {
    Page<Measurement> findBySensorId(Long sensorId, Pageable pageable);
    List<Measurement> findBySensorIdOrderByMeasuredAtDescIdDesc(Long sensorId);
    Optional<Measurement> findFirstBySensorIdOrderBySimulationStepDescIdDesc(Long sensorId);
    List<Measurement> findBySensorLocationIdAndSimulationStep(Long locationId, long simulationStep);
    long countBySensorId(Long sensorId);

    @EntityGraph(attributePaths = {"sensor", "sensor.location", "sensor.location.floor", "sensor.location.floor.building"})
    Page<Measurement> findAllByOrderByMeasuredAtDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"sensor", "sensor.location", "sensor.location.floor", "sensor.location.floor.building"})
    Page<Measurement> findBySensorLocationFloorBuildingIdOrderByMeasuredAtDescIdDesc(Long buildingId, Pageable pageable);

    @Query("select count(m), avg(m.value), min(m.value), max(m.value) from Measurement m where m.sensor.type = :type")
    Object[] aggregateAll(@Param("type") SensorType type);

    @Query("select count(m), avg(m.value), min(m.value), max(m.value) from Measurement m " +
           "where m.sensor.type = :type and m.sensor.location.floor.building.id = :buildingId")
    Object[] aggregateBuilding(@Param("type") SensorType type, @Param("buildingId") Long buildingId);

    long countBySensorLocationFloorBuildingId(Long buildingId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from Measurement m where m.sensor.location.id = :locationId")
    int deleteByLocationId(@org.springframework.data.repository.query.Param("locationId") Long locationId);

}

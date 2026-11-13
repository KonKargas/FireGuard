package com.kkargas.fireguard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kkargas.fireguard.entity.Sensor;
import com.kkargas.fireguard.enums.SensorType;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    List<Sensor> findByLocationIdOrderByTypeAsc(Long locationId);

    Optional<Sensor> findByLocationIdAndType(Long locationId, SensorType type);

    boolean existsByLocationIdAndType(Long locationId, SensorType type);

    boolean existsByCodeIgnoreCase(String code);

    long countByLocationId(Long locationId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from Sensor s where s.location.id = :locationId")
    int deleteByLocationId(@org.springframework.data.repository.query.Param("locationId") Long locationId);

}

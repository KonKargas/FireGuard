package com.kkargas.fireguard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kkargas.fireguard.entity.FireAlert;
import com.kkargas.fireguard.enums.AlertStatus;

public interface FireAlertRepository extends JpaRepository<FireAlert, Long> {
    Optional<FireAlert> findFirstByLocationIdAndStatusOrderByDetectedAtDesc(Long locationId, AlertStatus status);
    List<FireAlert> findByLocationIdOrderByDetectedAtDesc(Long locationId);
    List<FireAlert> findByStatusOrderByDetectedAtDesc(AlertStatus status);
    List<FireAlert> findByLocationFloorBuildingIdOrderByDetectedAtDesc(Long buildingId);
    long countByStatus(AlertStatus status);
    long countByLocationFloorBuildingId(Long buildingId);
    long countByLocationFloorBuildingIdAndStatus(Long buildingId, AlertStatus status);

    // Fetch the complete location hierarchy before leaving the repository/service transaction.
    @EntityGraph(attributePaths = {"location", "location.floor", "location.floor.building"})
    List<FireAlert> findAllByOrderByDetectedAtDesc();

    @EntityGraph(attributePaths = {"location", "location.floor", "location.floor.building"})
    @Query("select a from FireAlert a where a.id = :id")
    Optional<FireAlert> findDetailsById(@Param("id") Long id);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from FireAlert a where a.location.id = :locationId")
    int deleteByLocationId(@org.springframework.data.repository.query.Param("locationId") Long locationId);

}

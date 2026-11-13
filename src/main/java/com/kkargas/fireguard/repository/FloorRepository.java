package com.kkargas.fireguard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kkargas.fireguard.entity.Floor;

@Repository
public interface FloorRepository extends JpaRepository<Floor, Long> {

    List<Floor> findByBuildingIdOrderByLevelNumberAsc(Long buildingId);

    @EntityGraph(attributePaths = "building")
    @Query("select f from Floor f where f.id = :id")
    Optional<Floor> findDetailsById(@Param("id") Long id);


    Optional<Floor> findByBuildingIdAndLevelNumber(Long buildingId, int levelNumber);

    boolean existsByBuildingIdAndLevelNumber(Long buildingId, int levelNumber);

    long countByBuildingId(Long buildingId);
}

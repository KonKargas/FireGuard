package com.kkargas.fireguard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.kkargas.fireguard.entity.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByFloorIdOrderByCodeAsc(Long floorId);

    List<Location> findByFloorBuildingIdOrderByFloorLevelNumberAscCodeAsc(Long buildingId);

    Optional<Location> findByFloorIdAndCode(Long floorId, String code);

    boolean existsByFloorIdAndCode(Long floorId, String code);

    long countByFloorId(Long floorId);

    long countByFloorBuildingId(Long buildingId);
    
    @EntityGraph(attributePaths = {
    	    "floor",
    	    "floor.building"
    	})
    	@org.springframework.data.jpa.repository.Query(
    	    "SELECT l FROM Location l WHERE l.id = :id"
    	)
    	Optional<Location> findDetailsById(
    	    @org.springframework.data.repository.query.Param("id") Long id
    	);
}

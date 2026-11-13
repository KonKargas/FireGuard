package com.kkargas.fireguard.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kkargas.fireguard.entity.Building;
import com.kkargas.fireguard.enums.BuildingType;

@Repository
public interface BuildingRepository
        extends JpaRepository<Building, Long> {

    List<Building> findAllByOrderByNameAsc();

    List<Building> findByType(BuildingType type);

    boolean existsByNameIgnoreCase(String name);
}

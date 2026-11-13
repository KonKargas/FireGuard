package com.kkargas.fireguard.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kkargas.fireguard.dto.SimulationResult;
import com.kkargas.fireguard.entity.Building;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.service.BuildingService;
import com.kkargas.fireguard.service.LocationService;
import com.kkargas.fireguard.service.SimulationService;

@Controller
@RequestMapping("/simulation")
public class SimulationController {
    private final SimulationService simulation;
    private final BuildingService buildings;
    private final LocationService locations;

    public SimulationController(SimulationService simulation, BuildingService buildings,
                                LocationService locations) {
        this.simulation = simulation;
        this.buildings = buildings;
        this.locations = locations;
    }

    @GetMapping
    public String index(@RequestParam(required = false) Long buildingId, Model model) {
        List<Building> allBuildings = buildings.findAll();
        if (buildingId != null) buildings.get(buildingId);
        List<RoomOption> rooms = new ArrayList<>();
        for (Building building : allBuildings) {
            if (buildingId != null && !buildingId.equals(building.getId())) continue;
            for (Location summary : locations.findByBuilding(building.getId())) {
                Location room = locations.getDetails(summary.getId());
                rooms.add(new RoomOption(room.getId(), room.getCode(), room.getName(),
                        room.getFloor().getName(), building.getName()));
            }
        }
        model.addAttribute("page", "simulation");
        model.addAttribute("pageTitle", "Simulation");
        model.addAttribute("buildings", allBuildings);
        model.addAttribute("buildingId", buildingId);
        model.addAttribute("rooms", rooms);
        return "simulation/index";
    }

    @PostMapping("/location")
    public String runLocation(@RequestParam Long locationId, RedirectAttributes redirect) {
        try {
            SimulationResult result = simulation.refreshLocation(locationId);
            Location room = locations.getDetails(locationId);
            redirect.addFlashAttribute("results", List.of(toRow(result, room)));
            redirect.addFlashAttribute("success", "Location refreshed successfully.");
            return "redirect:/simulation?buildingId=" + room.getFloor().getBuilding().getId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/simulation";
        }
    }

    @PostMapping("/building")
    public String runBuilding(@RequestParam Long buildingId, RedirectAttributes redirect) {
        try {
            buildings.get(buildingId);
            List<SimulationResult> results = simulation.refreshBuilding(buildingId);
            List<ResultRow> rows = results.stream()
                    .map(result -> toRow(result, locations.getDetails(result.locationId())))
                    .toList();
            long detected = rows.stream().filter(ResultRow::fireDetected).count();
            redirect.addFlashAttribute("results", rows);
            redirect.addFlashAttribute("success", "Refreshed " + rows.size()
                    + " locations; fire detections: " + detected + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/simulation?buildingId=" + buildingId;
    }

    private ResultRow toRow(SimulationResult result, Location room) {
        return new ResultRow(room.getId(), room.getCode(), room.getName(),
                room.getFloor().getName(), room.getFloor().getBuilding().getName(),
                result.simulationStep(), result.temperature(), result.luminosity(),
                result.temperatureReliability() * 100,
                result.luminosityReliability() * 100,
                result.combinedReliability() * 100,
                result.fireDetected(), result.measuredAt().toString().replace('T', ' '));
    }

    public record RoomOption(Long id, String code, String name, String floorName, String buildingName) {}
    public record ResultRow(Long id, String code, String name, String floorName,
            String buildingName, long step, double temperature, double luminosity,
            double temperatureReliability, double luminosityReliability,
            double combinedReliability, boolean fireDetected, String measuredAt) {}
}

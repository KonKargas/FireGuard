package com.kkargas.fireguard.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kkargas.fireguard.entity.Building;
import com.kkargas.fireguard.entity.Floor;
import com.kkargas.fireguard.service.BuildingService;
import com.kkargas.fireguard.service.FloorService;
import com.kkargas.fireguard.repository.LocationRepository;

@Controller
@RequestMapping("/floors")
public class FloorsController {

    private final FloorService floors;
    private final BuildingService buildings;
    private final LocationRepository locations;

    public FloorsController(FloorService floors, BuildingService buildings, LocationRepository locations) {
        this.floors = floors;
        this.buildings = buildings;
        this.locations = locations;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long buildingId, Model model) {
        List<Building> allBuildings = buildings.findAll();
        List<FloorRow> selectedFloors;
        if (buildingId != null) {
            Building building = buildings.get(buildingId);
            model.addAttribute("selectedBuilding", building);
            selectedFloors = floorRows(building);
        } else {
            // Loading each building's floors happens inside the service's read-only transaction.
            // We use a small read-only view model to avoid lazy-load errors in Thymeleaf.
            selectedFloors = allBuildings.stream()
                    .flatMap(b -> floorRows(b).stream()).toList();
        }
        model.addAttribute("page", "floors");
        model.addAttribute("pageTitle", "Floors");
        model.addAttribute("buildings", allBuildings);
        model.addAttribute("floors", selectedFloors);
        model.addAttribute("selectedBuildingId", buildingId);
        return "floors/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long buildingId, Model model) {
        FloorForm input = new FloorForm();
        input.setBuildingId(buildingId);
        return form(model, input, null);
    }

    @PostMapping
    public String create(@ModelAttribute("form") FloorForm input,
                         Model model, RedirectAttributes redirect) {
        try {
            if (input.getBuildingId() == null) {
                throw new IllegalArgumentException("Please select a building.");
            }
            Floor created = floors.create(input.getBuildingId(), input.getLevelNumber(),
                    input.getName(), input.getElevationMeters(), input.getDescription());
            redirect.addFlashAttribute("success", "Floor created successfully.");
            return "redirect:/floors/" + created.getId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return form(model, input, null);
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        Floor floor = floors.getDetails(id);
        model.addAttribute("page", "floors");
        model.addAttribute("pageTitle", floor.getName());
        model.addAttribute("floor", floor);
        model.addAttribute("locations", locations.findByFloorIdOrderByCodeAsc(id));
        Building parent = floor.getBuilding();
        model.addAttribute("buildingId", parent.getId());
        model.addAttribute("buildingName", parent.getName());
        return "floors/details";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Floor floor = floors.getDetails(id);
        FloorForm input = new FloorForm();
        input.setBuildingId(floor.getBuilding().getId());
        input.setLevelNumber(floor.getLevelNumber());
        input.setName(floor.getName());
        input.setElevationMeters(floor.getElevationMeters());
        input.setDescription(floor.getDescription());
        return form(model, input, id);
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute("form") FloorForm input,
                       Model model, RedirectAttributes redirect) {
        try {
            floors.update(id, input.getLevelNumber(), input.getName(),
                    input.getElevationMeters(), input.getDescription());
            redirect.addFlashAttribute("success", "Floor updated successfully.");
            return "redirect:/floors/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return form(model, input, id);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) Long buildingId,
                         RedirectAttributes redirect) {
        try {
            // Lookup before deleting, to retain a valid return destination.
            Long parentId = floors.getDetails(id).getBuilding().getId();
            floors.deleteEmpty(id);
            redirect.addFlashAttribute("success", "Floor deleted successfully.");
            return "redirect:/floors?buildingId=" + parentId;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return buildingId != null ? "redirect:/floors?buildingId=" + buildingId : "redirect:/floors";
        }
    }

    private String form(Model model, FloorForm input, Long id) {
        model.addAttribute("page", "floors");
        model.addAttribute("pageTitle", id == null ? "New Floor" : "Edit Floor");
        model.addAttribute("form", input);
        model.addAttribute("floorId", id);
        model.addAttribute("buildings", buildings.findAll());
        return "floors/form";
    }

    private List<FloorRow> floorRows(Building building) {
        return floors.findByBuilding(building.getId()).stream()
                .map(f -> new FloorRow(f.getId(), f.getName(), f.getLevelNumber(),
                        f.getElevationMeters(), building.getId(), building.getName(),
                        locations.countByFloorId(f.getId())))
                .toList();
    }

    public record FloorRow(Long id, String name, int levelNumber,
            double elevationMeters, Long buildingId, String buildingName, long locationCount) {}


    public static class FloorForm {
        private Long buildingId;
        private int levelNumber;
        private String name;
        private double elevationMeters;
        private String description;
        public Long getBuildingId() { return buildingId; }
        public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }
        public int getLevelNumber() { return levelNumber; }
        public void setLevelNumber(int levelNumber) { this.levelNumber = levelNumber; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getElevationMeters() { return elevationMeters; }
        public void setElevationMeters(double elevationMeters) { this.elevationMeters = elevationMeters; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}

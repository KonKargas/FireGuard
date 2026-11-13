package com.kkargas.fireguard.controller;

import java.util.ArrayList;
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

import com.kkargas.fireguard.dto.LocationGeometry;
import com.kkargas.fireguard.enums.LocationType;
import com.kkargas.fireguard.entity.Building;
import com.kkargas.fireguard.entity.Floor;
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.service.BuildingService;
import com.kkargas.fireguard.service.FloorService;
import com.kkargas.fireguard.service.LocationService;
import com.kkargas.fireguard.service.SensorService;
import com.kkargas.fireguard.service.SpatialSetupService;

@Controller
@RequestMapping("/locations")
public class LocationsController {
    private final LocationService locations;
    private final FloorService floors;
    private final BuildingService buildings;
    private final SensorService sensors;
    private final SpatialSetupService spatialSetup;

    public LocationsController(LocationService locations, FloorService floors,
            BuildingService buildings, SensorService sensors, SpatialSetupService spatialSetup) {
        this.locations = locations;
        this.floors = floors;
        this.buildings = buildings;
        this.sensors = sensors;
        this.spatialSetup = spatialSetup;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long floorId, Model model) {
        List<RoomRow> rows = new ArrayList<>();
        for (Building building : buildings.findAll()) {
            for (Floor floor : floors.findByBuilding(building.getId())) {
                if (floorId != null && !floorId.equals(floor.getId())) continue;
                for (Location room : locations.findByFloor(floor.getId())) {
                    rows.add(row(room, floor, building));
                }
            }
        }
        if (floorId != null) floors.get(floorId);
        model.addAttribute("page", "locations");
        model.addAttribute("pageTitle", "Locations");
        model.addAttribute("rooms", rows);
        model.addAttribute("floorId", floorId);
        model.addAttribute("floorOptions", floorOptions());
        return "locations/list";
    }

    // Geometry-only data for the read-only preview in the Location form.
    @GetMapping("/floor-preview")
    @org.springframework.web.bind.annotation.ResponseBody
    public List<PreviewSpace> floorPreview(@RequestParam Long floorId) {
        floors.get(floorId); // Check the floor exists.
        return locations.findByFloor(floorId).stream()
                .map(r -> new PreviewSpace(r.getId(), r.getCode(), r.getName(),
                        r.getType().name(), r.getPositionX(), r.getPositionY(),
                        r.getPositionZ(), r.getWidthMeters(), r.getDepthMeters(),
                        r.getHeightMeters()))
                .toList();
    }

    public record PreviewSpace(Long id, String code, String name, String type,
            double x, double y, double z, double width, double depth, double height) {}

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long floorId, Model model) {
        RoomForm input = new RoomForm();
        input.setFloorId(floorId);
        return form(model, input, null);
    }

    @PostMapping
    public String create(@ModelAttribute("form") RoomForm input, Model model,
                         RedirectAttributes redirect) {
        try {
            if (input.getFloorId() == null) throw new IllegalArgumentException("Select a floor.");
            Location saved = spatialSetup.createReadyLocation(input.getFloorId(),
                    input.getCode(), input.getName(), input.getDescription(), input.geometry(),
                    input.getType(), input.isInstallSensors());
            redirect.addFlashAttribute("success", "Location created successfully.");
            return "redirect:/locations/" + saved.getId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return form(model, input, null);
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {

        Location room = locations.getDetails(id);

        Floor floor = room.getFloor();
        Building building = floor.getBuilding();

        model.addAttribute("page", "locations");
        model.addAttribute("pageTitle", room.getName());
        model.addAttribute("room", room);

        model.addAttribute("floorId", floor.getId());
        model.addAttribute("floorName", floor.getName());

        model.addAttribute("buildingId", building.getId());
        model.addAttribute("buildingName", building.getName());

        model.addAttribute("sensors", sensors.findByLocation(id));

        return "locations/details";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Location room = locations.get(id);
        RoomForm input = new RoomForm();
        input.setFloorId(room.getFloor().getId());
        input.setCode(room.getCode()); input.setName(room.getName());
        input.setDescription(room.getDescription());
        input.setType(room.getType());
        input.setX(room.getPositionX()); input.setY(room.getPositionY());
        input.setZ(room.getPositionZ()); input.setWidth(room.getWidthMeters());
        input.setDepth(room.getDepthMeters()); input.setHeight(room.getHeightMeters());
        return form(model, input, id);
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute("form") RoomForm input,
                       Model model, RedirectAttributes redirect) {
        try {
            locations.update(id, input.getCode(), input.getName(), input.getDescription(), input.geometry(), input.getType());
            redirect.addFlashAttribute("success", "Location updated.");
            return "redirect:/locations/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return form(model, input, id);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        Long floorId;
        try {
            floorId = locations.get(id).getFloor().getId();
            locations.deleteWithDependencies(id);
            redirect.addFlashAttribute("success", "Location, sensors, measurements and alerts permanently deleted.");
            return "redirect:/locations?floorId=" + floorId;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/locations/" + id;
        }
    }

    @PostMapping("/{id}/layout")
    public String updateLayout(@PathVariable Long id,
            @RequestParam double x, @RequestParam double y,
            @RequestParam double width, @RequestParam double depth,
            RedirectAttributes redirect) {
        Location room = locations.getDetails(id);
        Long floorId = room.getFloor().getId();
        try {
            locations.update(id, room.getCode(), room.getName(), room.getDescription(),
                    new LocationGeometry(x, y, room.getPositionZ(), width, depth,
                            room.getHeightMeters()), room.getType());
            redirect.addFlashAttribute("success", "Floor layout updated.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/locations/plan?floorId=" + floorId;
    }

    @GetMapping("/plan")
    public String plan(@RequestParam Long floorId, Model model) {
        Floor floor = floors.getDetails(floorId);
        Building building = buildings.get(floor.getBuilding().getId());
        List<RoomRow> rows = locations.findByFloor(floorId).stream()
                .map(r -> row(r, floor, building)).toList();
        model.addAttribute("page", "locations");
        model.addAttribute("pageTitle", "Floor Plan");
        model.addAttribute("floor", floor);
        model.addAttribute("buildingId", building.getId());
        model.addAttribute("rooms", rows);
        model.addAttribute("locationTypes", LocationType.values());
        return "locations/plan";
    }

    private String form(Model model, RoomForm input, Long id) {
        model.addAttribute("page", "locations");
        model.addAttribute("pageTitle", id == null ? "New Location" : "Edit Location");
        model.addAttribute("form", input);
        model.addAttribute("locationId", id);
        model.addAttribute("floorOptions", floorOptions());
        model.addAttribute("locationTypes", LocationType.values());
        return "locations/form";
    }

    private List<FloorOption> floorOptions() {
        List<FloorOption> opts = new ArrayList<>();
        for (Building b : buildings.findAll()) {
            for (Floor f : floors.findByBuilding(b.getId())) {
                opts.add(new FloorOption(f.getId(), b.getName() + " / " + f.getName()
                        + " (Level " + f.getLevelNumber() + ")"));
            }
        }
        return opts;
    }

    private RoomRow row(Location r, Floor f, Building b) {
        return new RoomRow(r.getId(), r.getCode(), r.getName(), f.getId(), f.getName(),
                b.getName(), r.getType().name(), r.getPositionX(), r.getPositionY(), r.getPositionZ(),
                r.getWidthMeters(), r.getDepthMeters(), r.getHeightMeters());
    }

    public record FloorOption(Long id, String label) {}
    public record RoomRow(Long id, String code, String name, Long floorId,
            String floorName, String buildingName, String type, double x, double y, double z,
            double width, double depth, double height) {}

    public static class RoomForm {
        private Long floorId;
        private String code, name, description;
        private LocationType type = LocationType.HOTEL_ROOM;
        private boolean installSensors = true;
        public LocationType getType() { return type; }
        public void setType(LocationType type) { this.type = type; }
        public boolean isInstallSensors() { return installSensors; }
        public void setInstallSensors(boolean value) { installSensors = value; }
        private double x, y, z, width = 4, depth = 5, height = 2.8;
        public LocationGeometry geometry() {
            return new LocationGeometry(x, y, z, width, depth, height);
        }
        public Long getFloorId() { return floorId; }
        public void setFloorId(Long v) { floorId = v; }
        public String getCode() { return code; }
        public void setCode(String v) { code = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public double getX() { return x; }
        public void setX(double v) { x = v; }
        public double getY() { return y; }
        public void setY(double v) { y = v; }
        public double getZ() { return z; }
        public void setZ(double v) { z = v; }
        public double getWidth() { return width; }
        public void setWidth(double v) { width = v; }
        public double getDepth() { return depth; }
        public void setDepth(double v) { depth = v; }
        public double getHeight() { return height; }
        public void setHeight(double v) { height = v; }
    }
}

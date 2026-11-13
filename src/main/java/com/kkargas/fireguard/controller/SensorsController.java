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
import com.kkargas.fireguard.entity.Location;
import com.kkargas.fireguard.entity.Sensor;
import com.kkargas.fireguard.repository.SensorRepository;
import com.kkargas.fireguard.service.LocationService;
import com.kkargas.fireguard.service.SensorService;

@Controller
@RequestMapping("/sensors")
public class SensorsController {
    private final SensorRepository repository;
    private final SensorService sensors;
    private final LocationService locations;

    public SensorsController(SensorRepository repository, SensorService sensors,
                             LocationService locations) {
        this.repository = repository;
        this.sensors = sensors;
        this.locations = locations;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long locationId, Model model) {
        List<Sensor> found = locationId == null ? repository.findAll() : sensors.findByLocation(locationId);
        List<SensorRow> rows = found.stream().map(this::toRow).toList();
        model.addAttribute("page", "sensors");
        model.addAttribute("pageTitle", "Sensors");
        model.addAttribute("rows", rows);
        model.addAttribute("locationId", locationId);
        return "sensors/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        Sensor sensor = sensors.get(id);
        Location room = locations.getDetails(sensor.getLocation().getId());
        model.addAttribute("page", "sensors");
        model.addAttribute("pageTitle", sensor.getCode());
        model.addAttribute("sensor", sensor);
        model.addAttribute("room", room);
        model.addAttribute("floor", room.getFloor());
        model.addAttribute("building", room.getFloor().getBuilding());
        return "sensors/details";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Sensor sensor = sensors.get(id);
        SensorPositionForm form = new SensorPositionForm();
        form.setX(sensor.getPositionX());
        form.setY(sensor.getPositionY());
        form.setZ(sensor.getPositionZ());
        return renderForm(model, sensor, form);
    }

    @PostMapping("/{id}/edit")
    public String move(@PathVariable Long id, @ModelAttribute("form") SensorPositionForm input,
                       Model model, RedirectAttributes redirect) {
        try {
            sensors.move(id, input.getX(), input.getY(), input.getZ());
            redirect.addFlashAttribute("success", "Sensor position updated.");
            return "redirect:/sensors/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return renderForm(model, sensors.get(id), input);
        }
    }

    private String renderForm(Model model, Sensor sensor, SensorPositionForm input) {
        Location room = locations.getDetails(sensor.getLocation().getId());
        model.addAttribute("page", "sensors");
        model.addAttribute("pageTitle", "Move Sensor");
        model.addAttribute("sensor", sensor);
        model.addAttribute("room", room);
        model.addAttribute("form", input);
        return "sensors/form";
    }

    private SensorRow toRow(Sensor sensor) {
        Location room = locations.getDetails(sensor.getLocation().getId());
        return new SensorRow(sensor.getId(), sensor.getCode(), sensor.getType().name(),
                room.getId(), room.getCode(), room.getName(),
                room.getFloor().getName(), room.getFloor().getBuilding().getName(),
                sensor.getPositionX(), sensor.getPositionY(), sensor.getPositionZ(),
                sensor.getRefreshCount(), sensor.getReliability() * 100.0);
    }

    public record SensorRow(Long id, String code, String type, Long locationId,
            String locationCode, String locationName, String floorName, String buildingName,
            double x, double y, double z, long refreshCount, double reliabilityPercent) { }

    public static class SensorPositionForm {
        private double x, y, z;
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
        public double getZ() { return z; }
        public void setZ(double z) { this.z = z; }
    }
}

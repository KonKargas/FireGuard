package com.kkargas.fireguard.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kkargas.fireguard.entity.Building;
import com.kkargas.fireguard.entity.Floor;
import com.kkargas.fireguard.enums.BuildingType;
import com.kkargas.fireguard.service.BuildingService;
import com.kkargas.fireguard.service.FloorService;

@Controller
@RequestMapping("/buildings")
public class BuildingsController {
    private final BuildingService buildings;
    private final FloorService floors;

    public BuildingsController(BuildingService buildings, FloorService floors) {
        this.buildings = buildings;
        this.floors = floors;
    }

    @ModelAttribute("buildingTypes")
    public BuildingType[] buildingTypes() {
        return BuildingType.values();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("page", "buildings");
        model.addAttribute("pageTitle", "Buildings");
        model.addAttribute("buildings", buildings.findAll());
        return "buildings/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        return form(model, new BuildingForm(), null);
    }

    @PostMapping
    public String create(@ModelAttribute BuildingForm input, Model model, RedirectAttributes flash) {
        try {
            Building created = buildings.create(input.getName(), input.getType(), input.getAddress(), input.getDescription());
            flash.addFlashAttribute("success", "Building created successfully.");
            return "redirect:/buildings/" + created.getId();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("error", exception.getMessage());
            return form(model, input, null);
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        Building building = buildings.get(id);
        List<Floor> floorList = floors.findByBuilding(id);
        model.addAttribute("page", "buildings");
        model.addAttribute("pageTitle", building.getName());
        model.addAttribute("building", building);
        model.addAttribute("floors", floorList);
        return "buildings/details";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Building building = buildings.get(id);
        BuildingForm input = new BuildingForm();
        input.setName(building.getName());
        input.setType(building.getType());
        input.setAddress(building.getAddress());
        input.setDescription(building.getDescription());
        return form(model, input, id);
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute BuildingForm input, Model model, RedirectAttributes flash) {
        try {
            buildings.update(id, input.getName(), input.getType(), input.getAddress(), input.getDescription());
            flash.addFlashAttribute("success", "Building updated successfully.");
            return "redirect:/buildings/" + id;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("error", exception.getMessage());
            return form(model, input, id);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            buildings.deleteEmpty(id);
            flash.addFlashAttribute("success", "Building deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/buildings";
    }

    private String form(Model model, BuildingForm input, Long id) {
        model.addAttribute("page", "buildings");
        model.addAttribute("pageTitle", id == null ? "New Building" : "Edit Building");
        model.addAttribute("form", input);
        model.addAttribute("buildingId", id);
        return "buildings/form";
    }

    public static class BuildingForm {
        private String name;
        private BuildingType type;
        private String address;
        private String description;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BuildingType getType() { return type; }
        public void setType(BuildingType type) { this.type = type; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}

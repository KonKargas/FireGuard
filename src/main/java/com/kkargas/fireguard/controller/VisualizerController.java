package com.kkargas.fireguard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.kkargas.fireguard.dto.Building3DDto;
import com.kkargas.fireguard.repository.BuildingRepository;
import com.kkargas.fireguard.service.VisualizerService;

@Controller
@RequestMapping("/visualizer")
public class VisualizerController {
    private final BuildingRepository buildings;
    private final VisualizerService visualizer;

    public VisualizerController(BuildingRepository buildings, VisualizerService visualizer) {
        this.buildings = buildings;
        this.visualizer = visualizer;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("page", "visualizer");
        model.addAttribute("pageTitle", "3D Building Visualizer");
        model.addAttribute("buildings", buildings.findAllByOrderByNameAsc());
        return "visualizer/index";
    }

    @GetMapping("/api/buildings/{id}")
    @ResponseBody
    public Building3DDto building(@PathVariable Long id) {
        return visualizer.building(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<String> notFound(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}

package com.kkargas.fireguard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kkargas.fireguard.repository.BuildingRepository;
import com.kkargas.fireguard.service.AlertHistoryService;

@Controller
@RequestMapping("/alerts")
public class AlertsController {
    private final AlertHistoryService history;
    private final BuildingRepository buildings;

    public AlertsController(AlertHistoryService history, BuildingRepository buildings) {
        this.history = history;
        this.buildings = buildings;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "ALL") String status,
                       @RequestParam(required = false) Long buildingId, Model model) {
        if (buildingId != null && !buildings.existsById(buildingId)) {
            throw new IllegalArgumentException("Building not found: " + buildingId);
        }
        model.addAttribute("page", "alerts");
        model.addAttribute("pageTitle", "Fire Alerts");
        model.addAttribute("alerts", history.findAlerts(status, buildingId));
        model.addAttribute("status", status.toUpperCase(java.util.Locale.ROOT));
        model.addAttribute("buildingId", buildingId);
        model.addAttribute("buildings", buildings.findAllByOrderByNameAsc());
        model.addAttribute("activeCount", history.countActive());
        model.addAttribute("resolvedCount", history.countResolved());
        model.addAttribute("totalCount", history.countAll());
        return "alerts/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("page", "alerts");
        model.addAttribute("pageTitle", "Alert Details");
        model.addAttribute("alert", history.getDetails(id));
        return "alerts/details";
    }
}

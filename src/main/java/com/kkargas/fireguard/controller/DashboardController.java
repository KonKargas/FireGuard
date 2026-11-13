package com.kkargas.fireguard.controller;

import com.kkargas.fireguard.repository.BuildingRepository;
import com.kkargas.fireguard.repository.FloorRepository;
import com.kkargas.fireguard.repository.LocationRepository;
import com.kkargas.fireguard.repository.SensorRepository;
import com.kkargas.fireguard.repository.MeasurementRepository;
import com.kkargas.fireguard.repository.FireAlertRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final BuildingRepository buildings;
    private final FloorRepository floors;
    private final LocationRepository locations;
    private final SensorRepository sensors;
    private final MeasurementRepository measurements;
    private final FireAlertRepository alerts;

    public DashboardController(BuildingRepository buildings, FloorRepository floors,
                               LocationRepository locations, SensorRepository sensors,
                               MeasurementRepository measurements, FireAlertRepository alerts) {
        this.buildings = buildings;
        this.floors = floors;
        this.locations = locations;
        this.sensors = sensors;
        this.measurements = measurements;
        this.alerts = alerts;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("page", "dashboard");
        model.addAttribute("pageTitle", "Overview");
        model.addAttribute("buildingsCount", buildings.count());
        model.addAttribute("floorsCount", floors.count());
        model.addAttribute("locationsCount", locations.count());
        model.addAttribute("sensorsCount", sensors.count());
        model.addAttribute("measurementsCount", measurements.count());
        model.addAttribute("alertsCount", alerts.count());
        return "dashboard";
    }
}

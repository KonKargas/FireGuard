package com.kkargas.fireguard.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.kkargas.fireguard.repository.BuildingRepository;
import com.kkargas.fireguard.service.ReportsService;
import com.kkargas.fireguard.service.ReportsService.Reading;

@Controller
@RequestMapping("/reports")
public class ReportsController {
    private final ReportsService reports;
    private final BuildingRepository buildings;
    public ReportsController(ReportsService reports,BuildingRepository buildings) {
        this.reports=reports;this.buildings=buildings;
    }
    @GetMapping
    public String index(@RequestParam(required=false) Long buildingId,Model model) {
        model.addAttribute("page","reports");
        model.addAttribute("pageTitle","Reports");
        model.addAttribute("buildings",buildings.findAllByOrderByNameAsc());
        model.addAttribute("buildingId",buildingId);
        model.addAttribute("report",reports.snapshot(buildingId,120));
        return "reports/index";
    }
    @GetMapping(value="/measurements.csv",produces="text/csv")
    public ResponseEntity<byte[]> csv(@RequestParam(required=false) Long buildingId) {
        var report=reports.snapshot(buildingId,5000);
        StringBuilder b=new StringBuilder("id,measured_at,building,floor,location,sensor,type,step,value,reliability\r\n");
        for (Reading m:report.recentReadings()) {
            b.append(m.id()).append(',').append(csv(m.measuredAt().toString())).append(',')
              .append(csv(m.buildingName())).append(',').append(csv(m.floorName())).append(',')
              .append(csv(m.locationCode())).append(',').append(csv(m.sensorCode())).append(',')
              .append(csv(m.sensorType())).append(',').append(m.simulationStep()).append(',')
              .append(m.value()).append(',').append(m.reliability()).append("\r\n");
        }
        byte[] bytes=("\uFEFF"+b).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=fireguard-measurements.csv")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(bytes);
    }
    // Escape formulas as well as CSV quotes to avoid spreadsheet formula execution.
    private static String csv(String input) {
        if(input==null) return "";
        String s=input.replace("\r"," ").replace("\n"," ").replace("","");
        if(!s.isEmpty() && "=+-@".indexOf(s.charAt(0))>=0) s="'"+s;
        return ""+s+"";
    }
}

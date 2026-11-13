package com.kkargas.fireguard.dto;

import java.util.List;

public record Building3DDto(Long id, String name, List<Floor3D> floors) {
    public record Floor3D(Long id, int level, String name, double elevation, List<Room3D> rooms) {}
    public record Room3D(Long id, String code, String name, String type, double x, double y, double z,
                         double width, double depth, double height, boolean activeAlert,
                         List<Sensor3D> sensors) {}
    public record Sensor3D(Long id, String code, String type, double x, double y, double z,
                           double reliability, long refreshCount) {}
}

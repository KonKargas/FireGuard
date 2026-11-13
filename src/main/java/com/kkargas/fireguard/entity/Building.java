package com.kkargas.fireguard.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.kkargas.fireguard.enums.BuildingType;
import jakarta.persistence.*;

@Entity
@Table(name = "buildings")
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BuildingType type;

    @Column(length = 255)
    private String address;

    @Column(length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Floor> floors = new ArrayList<>();

    public Building() {}

    public Building(String name, BuildingType type, String address, String description) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.description = description;
    }

    // Copy only scalar properties; do not duplicate persistent child entities.
    public Building(Building other) {
        this(other.name, other.type, other.address, other.description);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BuildingType getType() { return type; }
    public void setType(BuildingType type) { this.type = type; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Floor> getFloors() { return floors; }

    public void addFloor(Floor floor) {
        Objects.requireNonNull(floor, "Floor cannot be null");
        if (floor.getBuilding() != null && floor.getBuilding() != this)
            throw new IllegalArgumentException("Floor belongs to another building");
        if (!floors.contains(floor)) floors.add(floor);
        floor.setBuilding(this);
    }

    public void removeFloor(Floor floor) {
        if (floors.remove(floor)) floor.setBuilding(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Building other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "Building{id=" + id + ", name='" + name + "', type=" + type + "}";
    }
}

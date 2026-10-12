package com.fleetops.flota.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    private String id;

    @Column(nullable = false)
    private String plate;

    protected Vehicle() {
    }

    public Vehicle(String id, String plate) {
        this.id = id;
        this.plate = plate;
    }

    public String getId() { return id; }
    public String getPlate() { return plate; }
}

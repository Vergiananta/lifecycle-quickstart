package org.acme.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VehicleDto {
    public Long id;

    @JsonProperty("plate_number")
    public String plateNumber;

    public String type;
    public String brand;
    public String model;
    public Integer year;

    public VehicleDto() {}

    public VehicleDto(Long id, String plateNumber, String type, String brand, String model, Integer year) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.year = year;
    }
}


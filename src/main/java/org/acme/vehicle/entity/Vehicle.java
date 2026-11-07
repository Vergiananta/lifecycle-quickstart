package org.acme.vehicle.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "vehicles")
public class Vehicle extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "plate_number", unique = true, nullable = false)
    public String plateNumber;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public String brand;

    @Column(nullable = false)
    public String model;

    @Column(nullable = false)
    public Integer year;
}


package org.acme.vehicle.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inspections")
public class Inspection extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "vehicle_id", nullable = false)
    public Long vehicleId;

    @Column(name = "client_id", unique = true)
    public UUID clientId;

    @Column(name = "inspector_name", nullable = false)
    public String inspectorName;

    @Column(name = "has_key", nullable = false)
    public Boolean hasKey = true;

    @Column(nullable = false)
    public String status; // DRAFT, SUBMITTED, APPROVED, RECHECK

    @Column(nullable = false)
    public Integer version = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    public List<InspectionItem> items = new ArrayList<>();
}


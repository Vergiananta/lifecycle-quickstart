package org.acme.vehicle.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inspection_items")
public class InspectionItem extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", nullable = false)
    @JsonBackReference
    public Inspection inspection;

    @Column(name = "point_code", nullable = false)
    public String pointCode;

    @Column(nullable = false)
    public String condition; // GOOD, MINOR_DAMAGE, MAJOR_DAMAGE, NOT_INSPECTABLE

    public String note;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "photo_refs", columnDefinition = "text[]")
    public List<String> photoRefs = new ArrayList<>();
}


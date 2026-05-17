package com.sportify.field.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "fields")
@Getter @Setter @NoArgsConstructor
public class Field extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    public Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_type_id", nullable = false)
    public FieldType fieldType;

    @Column(nullable = false, length = 50)
    public String name;

    @Column(name = "image_url", length = 500)
    public String imageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Status status = Status.AVAILABLE;

    public enum Status {
        AVAILABLE, MAINTENANCE
    }

    // ── Finders ────────────────────────────────────────────────────────────
    public static List<Field> findByLocation(Long locationId) {
        return list("location.id", locationId);
    }

    public static List<Field> findAvailableByLocation(Long locationId) {
        return list("location.id = ?1 and status = ?2", locationId, Status.AVAILABLE);
    }
}

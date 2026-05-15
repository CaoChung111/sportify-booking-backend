package com.sportify.field.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "field_types")
@Getter @Setter @NoArgsConstructor
public class FieldType extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false)
    public Sport sport;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(name = "player_capacity", nullable = false)
    public Integer playerCapacity;
}

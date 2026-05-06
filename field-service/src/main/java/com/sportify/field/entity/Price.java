package com.sportify.field.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "prices")
@Getter @Setter @NoArgsConstructor
public class Price extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    public Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_type_id", nullable = false)
    public FieldType fieldType;

    @Column(name = "start_time", nullable = false)
    public LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    public LocalTime endTime;

    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    public DayType dayType;

    public enum DayType {
        WEEKDAY, WEEKEND, HOLIDAY
    }

    public static Price findApplicable(Long locationId, Long fieldTypeId, LocalTime time, DayType dayType) {
        return find(
            "location.id = ?1 AND fieldType.id = ?2 AND startTime <= ?3 AND endTime > ?3 AND dayType = ?4",
            locationId, fieldTypeId, time, dayType
        ).firstResult();
    }
}

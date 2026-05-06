package com.sportify.field.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "locations")
@Getter @Setter @NoArgsConstructor
public class Location extends PanacheEntity {

    @Column(nullable = false, length = 100)
    public String name;

    @Column(nullable = false)
    public String address;

    @Column(nullable = false, length = 50)
    public String region;

    @Column(length = 20)
    public String hotline;
}

package com.sportify.field.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sports")
@Getter @Setter @NoArgsConstructor
public class Sport extends PanacheEntity {

    @Column(nullable = false, length = 50)
    public String name;

    @Column(nullable = false, unique = true, length = 50)
    public String slug;

    public static Sport findBySlug(String slug) {
        return find("slug", slug).firstResult();
    }
}

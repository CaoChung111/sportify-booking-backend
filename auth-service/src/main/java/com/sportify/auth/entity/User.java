package com.sportify.auth.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 50)
    public String username;

    @Column(nullable = false, unique = true, length = 100)
    public String email;

    @Column(unique = true, length = 20)
    public String phone;

    @Column(name = "full_name", length = 100)
    public String fullName;

    /**
     * Keycloak User ID (sub claim in JWT).
     * Link between local profile and Keycloak identity.
     */
    @Column(name = "keycloak_id", unique = true, length = 100)
    public String keycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Status status = Status.ACTIVE;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Status {
        ACTIVE, INACTIVE
    }

    // ── Finders ────────────────────────────────────────────────────────────
    public static User findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public static User findByKeycloakId(String keycloakId) {
        return find("keycloakId", keycloakId).firstResult();
    }
}

package com.sportify.payment.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor
public class Payment extends PanacheEntity {

    /**
     * ID reference — không @ManyToOne cross-service.
     */
    @Column(name = "booking_id", nullable = false, unique = true)
    public Long bookingId;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    public PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    public PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Transaction reference từ payment gateway (VNPay/MoMo).
     * Dùng để idempotency check.
     */
    @Column(name = "txn_ref", unique = true, length = 100)
    public String txnRef;

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum PaymentMethod {
        CASH, VNPAY, MOMO
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, REFUNDED
    }

    // ── Finders ────────────────────────────────────────────────────────────
    public static Payment findByBookingId(Long bookingId) {
        return find("bookingId", bookingId).firstResult();
    }

    public static List<Payment> findByUserId(Long userId) {
        return list("userId = ?1 ORDER BY createdAt DESC", userId);
    }
}

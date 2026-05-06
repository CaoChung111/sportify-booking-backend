package com.sportify.booking.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor
public class Booking extends PanacheEntity {

    /**
     * ID reference — không dùng @ManyToOne cross-service.
     * Lưu thêm snapshot để tránh phụ thuộc vào field-service khi đọc.
     */
    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "field_id", nullable = false)
    public Long fieldId;

    // ── Snapshot tại thời điểm đặt sân ───────────────────────────────────
    @Column(name = "field_name", nullable = false, length = 100)
    public String fieldName;

    @Column(name = "location_name", nullable = false, length = 100)
    public String locationName;

    // ── Booking info ───────────────────────────────────────────────────────
    @Column(name = "booking_date", nullable = false)
    public LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    public LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    public LocalTime endTime;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    public BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public BookingStatus status = BookingStatus.PENDING;

    @Version
    public Integer version; // Optimistic locking — tránh double-booking

    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        PENDING, CONFIRMED, COMPLETED, CANCELLED
    }

    // ── Finders ────────────────────────────────────────────────────────────
    public static List<Booking> findByUserId(Long userId) {
        return list("userId = ?1 ORDER BY createdAt DESC", userId);
    }

    public static boolean hasConflict(Long fieldId, LocalDate date, LocalTime start, LocalTime end) {
        long count = count(
            "fieldId = ?1 AND bookingDate = ?2 AND status != ?3 " +
            "AND (startTime < ?5 AND endTime > ?4)",
            fieldId, date, BookingStatus.CANCELLED, start, end
        );
        return count > 0;
    }
}

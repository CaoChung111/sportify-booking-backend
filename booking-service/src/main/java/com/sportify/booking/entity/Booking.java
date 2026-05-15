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

    // Thời gian (phút) mà một đơn PENDING được coi là hợp lệ trước khi hết hạn
    public static final int PENDING_EXPIRATION_MINUTES = 15;

    /**
     * ID reference — không dùng @ManyToOne cross-service.
     * Mỗi service có DB riêng, không có FK xuyên service.
     */
    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "field_id", nullable = false)
    public Long fieldId;

    // ── Snapshot tại thời điểm đặt sân (tránh phụ thuộc field-service khi đọc)
    @Column(name = "field_name", nullable = false, length = 100)
    public String fieldName;

    @Column(name = "location_name", nullable = false, length = 100)
    public String locationName;

    // ── Booking info ───────────────────────────────────────────────────────────
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

    /** Ghi chú của khách */
    @Column(name = "note", columnDefinition = "TEXT")
    public String note;

    /**
     * Optimistic locking — phòng tránh 2 request đặt cùng slot đồng thời.
     * Nếu 2 transaction cùng đọc version=0 → 1 sẽ thắng, 1 sẽ bị OptimisticLockException.
     */
    @Version
    public Integer version;

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

    public enum BookingStatus {
        PENDING,    // Đặt chỗ, chờ thanh toán
        CONFIRMED,  // Đã thanh toán, chờ chơi
        COMPLETED,  // Đã chơi xong
        CANCELLED   // Đã huỷ
    }

    // ── Finders ────────────────────────────────────────────────────────────────

    public static List<Booking> findByUserId(Long userId) {
        return list("userId = ?1 ORDER BY createdAt DESC", userId);
    }

    public static List<Booking> findByFieldId(Long fieldId) {
        return list("fieldId = ?1 ORDER BY bookingDate DESC, startTime ASC", fieldId);
    }

    public static List<Booking> findByFieldIdAndDate(Long fieldId, LocalDate date) {
        return list(
                "fieldId = ?1 AND bookingDate = ?2 AND status != ?3 ORDER BY startTime ASC",
                fieldId, date, BookingStatus.CANCELLED);
    }

    public static List<Booking> findByStatus(String status) {
        return list("status = ?1 ORDER BY createdAt DESC", BookingStatus.valueOf(status));
    }

    /**
     * Pessimistic Lock: Khoá tất cả row booking của field + ngày này lại trước khi INSERT.
     *
     * Cơ chế:
     * - Transaction 1 giữ lock → Transaction 2 BỊ BLOCK tại đây.
     * - Khi T1 commit/rollback → T2 mới được tiếp tục, sau đó chạy hasConflict() lần nữa.
     * - Loại bỏ hoàn toàn race condition mà không cần Redis.
     *
     * QUAN TRỌNG: Phải gọi trong @Transactional context.
     */
    public static void lockSlot(Long fieldId, LocalDate date) {
        getEntityManager().createNativeQuery(
            "SELECT id FROM bookings " +
            "WHERE field_id = :fieldId AND booking_date = :date " +
            "AND status != 'CANCELLED' FOR UPDATE"
        )
        .setParameter("fieldId", fieldId)
        .setParameter("date", date)
        .getResultList(); // Kết quả không cần dùng — chỉ cần tác dụng LOCK
    }

    /**
     * Kiểm tra xung đột lịch đặt (Double Booking Prevention).
     * Một khung giờ bị coi là đã có người đặt nếu tồn tại một booking khác (B)
     * thoả mãn ĐỒNG THỜI 2 điều kiện:
     * 1. Khung giờ của B chồng chéo với khung giờ đang xét.
     * 2. Trạng thái của B là:
     *    - CONFIRMED hoặc COMPLETED
     *    - HOẶC là PENDING và chưa hết hạn (được tạo trong vòng 15 phút gần nhất).
     */
    public static boolean hasConflict(Long fieldId, LocalDate date, LocalTime start, LocalTime end) {
        LocalDateTime pendingCutoff = LocalDateTime.now().minusMinutes(PENDING_EXPIRATION_MINUTES);

        long count = count(
            "fieldId = ?1 AND bookingDate = ?2 " +
            "AND (startTime < ?4 AND endTime > ?3) " + // Điều kiện overlap
            "AND ( " +
            "  status = 'CONFIRMED' OR status = 'COMPLETED' OR " +
            "  (status = 'PENDING' AND createdAt >= ?5) " +
            ")",
            fieldId, date, start, end, pendingCutoff
        );
        return count > 0;
    }

    /**
     * Tìm tất cả booking PENDING quá hạn (dùng cho Scheduler auto-cancel).
     * @param cutoff Ngưỡng thời gian — booking có createdAt trước mốc này sẽ bị huỷ.
     */
    public static List<Booking> findExpiredPending(LocalDateTime cutoff) {
        return list("status = ?1 AND createdAt < ?2",
                BookingStatus.PENDING, cutoff);
    }
}

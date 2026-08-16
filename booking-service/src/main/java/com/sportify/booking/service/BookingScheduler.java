package com.sportify.booking.service;

import com.sportify.booking.client.PaymentServiceClient;
import com.sportify.booking.entity.Booking;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler tự động huỷ các đơn đặt sân PENDING bị "kẹt" (người dùng bỏ giữa chừng).
 *
 * Vấn đề được giải quyết:
 *   Khi người dùng tạo đơn nhưng không hoàn tất thanh toán, booking sẽ ở PENDING mãi mãi.
 *   Hàm hasConflict() hiểu PENDING = đã chiếm chỗ → không ai khác đặt được slot đó.
 *
 * Giải pháp:
 *   Chạy mỗi phút, quét DB, cập nhật CANCELLED tất cả booking PENDING
 *   có createdAt cũ hơn `booking.pending.expire-minutes` phút.
 */
@ApplicationScoped
public class BookingScheduler {

    private static final Logger LOG = Logger.getLogger(BookingScheduler.class);

    @Inject
    @RestClient
    PaymentServiceClient paymentServiceClient;

    /** Thời gian tối đa (phút) một booking được giữ ở trạng thái PENDING. */
    @ConfigProperty(name = "booking.pending.expire-minutes", defaultValue = "15")
    int expireMinutes;

    /**
     * Chạy mỗi 1 phút.
     * Tìm tất cả booking PENDING quá hạn và chuyển về CANCELLED.
     *
     * @Transactional — toàn bộ batch update nằm trong 1 transaction,
     *   đảm bảo tính nhất quán: hoặc tất cả được huỷ, hoặc không cái nào bị huỷ.
     */
    @Scheduled(every = "1m", identity = "booking-auto-cancel")
    @Transactional
    public void autoCancelExpiredBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expireMinutes);

        List<Booking> expired = Booking.findExpiredPending(cutoff);

        if (expired.isEmpty()) {
            return; // Không có gì cần xử lý — tránh log thừa
        }

        LOG.infof("[AutoCancel] Tìm thấy %d booking PENDING quá %d phút. Đang huỷ...",
                expired.size(), expireMinutes);

        for (Booking booking : expired) {
            if (!shouldCancelExpiredBooking(booking)) {
                continue;
            }

            booking.status = Booking.BookingStatus.CANCELLED;
            booking.persist();
            LOG.debugf("[AutoCancel] Đã huỷ booking #%d (user=%d, field=%d, date=%s, slot=%s-%s)",
                    booking.id, booking.userId, booking.fieldId,
                    booking.bookingDate, booking.startTime, booking.endTime);
        }

        LOG.infof("[AutoCancel] Hoàn thành — đã huỷ %d booking.", expired.size());
    }

    private boolean shouldCancelExpiredBooking(Booking booking) {
        try {
            var paymentResponse = paymentServiceClient.getByBookingId(booking.id);
            if (paymentResponse == null || paymentResponse.getData() == null) {
                return true;
            }

            var payment = paymentResponse.getData();
            return !"SUCCESS".equalsIgnoreCase(payment.paymentStatus());
        } catch (Exception e) {
            LOG.warnf("[AutoCancel] Không lấy được payment cho booking #%d (lỗi: %s), tiến hành huỷ đơn.", booking.id, e.getMessage());
            return true;
        }
    }
}

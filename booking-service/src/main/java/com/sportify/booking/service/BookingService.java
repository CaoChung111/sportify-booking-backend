package com.sportify.booking.service;

import com.sportify.booking.client.FieldServiceClient;
import com.sportify.booking.dto.BookingDto;
import com.sportify.booking.entity.Booking;
import com.sportify.common.exception.ServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class BookingService {

    @Inject
    @RestClient
    FieldServiceClient fieldServiceClient;

    // ── Tạo Đơn Đặt Sân ──────────────────────────────────────────────────────

    /**
     * Luồng tạo booking (Real-time):
     *
     * 1. Validate input: endTime > startTime, booking time is in the future
     * 2. Gọi field-service → kiểm tra sân tồn tại & lấy snapshot
     * 3. Kiểm tra sân đang AVAILABLE (không bảo trì)
     * 4. Kiểm tra xung đột lịch trong DB nội bộ (Double Booking Prevention)
     * 5. Gọi field-service → tính giá (Dynamic Pricing)
     * 6. Tạo Booking với status = PENDING
     */
    @Transactional
    public BookingDto.BookingResponse create(Long userId, BookingDto.CreateBookingRequest request) {
        // Bước 1: Validate thời gian
        if (!request.endTime.isAfter(request.startTime)) {
            throw ServiceException.badRequest("endTime must be after startTime");
        }
        if (request.bookingDate == null) {
            throw ServiceException.badRequest("bookingDate is required");
        }
        // **LOGIC MỚI:** Nếu đặt cho ngày hôm nay, giờ bắt đầu phải ở trong tương lai
        if (request.bookingDate.isEqual(LocalDate.now()) && request.startTime.isBefore(LocalTime.now())) {
            throw ServiceException.badRequest("Cannot book a time slot that has already passed today");
        }

        // Bước 2: Lấy thông tin sân từ field-service
        var fieldResponse = fieldServiceClient.getField(request.fieldId);
        if (fieldResponse == null || fieldResponse.getData() == null) {
            throw ServiceException.notFound("Field", request.fieldId);
        }
        var fieldDetail = fieldResponse.getData();

        // Bước 3: Kiểm tra trạng thái sân
        // Gọi API mới của field-service để kiểm tra trạng thái vận hành
        var availResp = fieldServiceClient.checkAvailability(request.fieldId);
        if (availResp == null || !Boolean.TRUE.equals(availResp.getData())) {
            throw ServiceException.badRequest(
                    "Field '" + fieldDetail.name() + "' is currently under maintenance and cannot be booked");
        }

        // Bước 4 — Pessimistic Lock + Kiểm tra xung đột lịch (Double Booking)
        //
        // Pattern "Lock-then-Check":
        //  1. lockSlot() giữ SELECT FOR UPDATE trên tất cả row của field+date này.
        //     → Nếu có T2 đến đồng thời, T2 sẽ bị MySQL BLOCK tại đây cho đến khi T1 commit.
        //  2. Sau khi lock thành công, chạy hasConflict() để kiểm tra thật sự.
        //     → T2 sau khi được giải phóng cũng chạy lại hasConflict() và thấy đã bị chiếm → 409.
        Booking.lockSlot(request.fieldId, request.bookingDate);
        if (Booking.hasConflict(request.fieldId, request.bookingDate, request.startTime, request.endTime)) {
            throw ServiceException.conflict(
                    "Time slot " + request.startTime + "–" + request.endTime +
                    " on " + request.bookingDate + " is already booked for this field");
        }

        // Bước 5: Tính giá
        String dateStr  = request.bookingDate.toString();
        String startStr = request.startTime.toString();
        String endStr   = request.endTime.toString();
        var priceResp = fieldServiceClient.calculatePrice(request.fieldId, dateStr, startStr, endStr);
        if (priceResp == null || priceResp.getData() == null) {
            throw ServiceException.badRequest("Cannot calculate price for this field and time slot");
        }
        var priceDetail = priceResp.getData();

        // Bước 6: Tạo Booking
        Booking booking       = new Booking();
        booking.userId        = userId;
        booking.fieldId       = request.fieldId;
        booking.fieldName     = fieldDetail.name();
        booking.locationName  = fieldDetail.locationName();
        booking.bookingDate   = request.bookingDate;
        booking.startTime     = request.startTime;
        booking.endTime       = request.endTime;
        booking.totalPrice    = priceDetail.totalPrice();
        booking.status        = Booking.BookingStatus.PENDING;
        booking.note          = request.note;
        booking.persist();

        return toResponse(booking);
    }

    // ── Lấy Danh Sách Đặt Sân Của User ───────────────────────────────────────

    public List<BookingDto.BookingResponse> getMyBookings(Long userId) {
        return Booking.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<BookingDto.BookingResponse> getBookingsByFieldAndDate(Long fieldId, LocalDate date) {
        if (fieldId == null) {
            throw ServiceException.badRequest("fieldId is required");
        }
        if (date == null) {
            throw ServiceException.badRequest("date is required");
        }

        return Booking.findByFieldIdAndDate(fieldId, date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Lấy Chi Tiết Một Booking ──────────────────────────────────────────────

    /**
     * Lấy chi tiết booking — kiểm tra quyền sở hữu (chỉ chủ đơn mới xem được).
     */
    public BookingDto.BookingResponse getById(Long id, Long userId) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);
        if (!booking.userId.equals(userId)) {
            throw ServiceException.badRequest("You do not have permission to view this booking");
        }
        return toResponse(booking);
    }

    // ── Huỷ Đặt Sân ──────────────────────────────────────────────────────────

    /**
     * Huỷ đặt sân:
     * - Chỉ chủ đơn mới được phép huỷ
     * - Chỉ được huỷ khi đang PENDING (chưa thanh toán)
     * - Đơn đã CONFIRMED (đã thanh toán) KHÔNG được huỷ qua endpoint này
     *   (cần nghiệp vụ hoàn tiền riêng)
     */
    @Transactional
    public void cancel(Long id, Long userId) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);

        if (!booking.userId.equals(userId)) {
            throw ServiceException.badRequest("You do not have permission to cancel this booking");
        }

        if (booking.status == Booking.BookingStatus.CONFIRMED) {
            throw ServiceException.badRequest(
                    "Cannot cancel a confirmed booking. Please contact support for refund.");
        }

        if (booking.status == Booking.BookingStatus.CANCELLED) {
            throw ServiceException.badRequest("Booking is already cancelled");
        }

        if (booking.status == Booking.BookingStatus.COMPLETED) {
            throw ServiceException.badRequest("Cannot cancel a completed booking");
        }

        booking.status = Booking.BookingStatus.CANCELLED;
        booking.persist();
    }

    // ── Xác Nhận Booking (Internal — gọi bởi Payment Service) ────────────────

    /**
     * Xác nhận booking sau khi thanh toán thành công.
     * QUAN TRỌNG: Endpoint này chỉ được Payment Service gọi,
     * không phải user. Trong production cần bảo vệ bằng mTLS hoặc internal network.
     *
     * Luồng: Payment SUCCESS → Payment Service gọi API này → Booking = CONFIRMED
     */
    @Transactional
    public void confirm(Long id) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);

        if (booking.status != Booking.BookingStatus.PENDING) {
            throw ServiceException.badRequest(
                    "Booking cannot be confirmed: current status is " + booking.status);
        }

        booking.status = Booking.BookingStatus.CONFIRMED;
        booking.persist();
    }

    // ── Hoàn thành Booking (cập nhật sau khi chơi xong) ──────────────────────

    @Transactional
    public void complete(Long id) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);

        if (booking.status != Booking.BookingStatus.CONFIRMED) {
            throw ServiceException.badRequest(
                    "Only CONFIRMED bookings can be marked as completed");
        }

        booking.status = Booking.BookingStatus.COMPLETED;
        booking.persist();
    }

    // ── Internal: Lấy Booking Không Kiểm Tra UserId ───────────────────────────

    /**
     * Dành cho Payment Service gọi nội bộ để đọc thông tin booking
     * mà không cần kiểm tra quyền sở hữu user.
     */
    public BookingDto.BookingResponse getByIdInternal(Long id) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);
        return toResponse(booking);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BookingDto.BookingResponse toResponse(Booking b) {
        BookingDto.BookingResponse r = new BookingDto.BookingResponse();
        r.id            = b.id;
        r.userId        = b.userId;
        r.fieldId       = b.fieldId;
        r.fieldName     = b.fieldName;
        r.locationName  = b.locationName;
        r.bookingDate   = b.bookingDate;
        r.startTime     = b.startTime;
        r.endTime       = b.endTime;
        r.durationHours = b.startTime != null && b.endTime != null
                ? Duration.between(b.startTime, b.endTime).toMinutes() / 60.0
                : 0;
        r.totalPrice    = b.totalPrice;
        r.status        = b.status.name();
        r.note          = b.note;
        r.createdAt     = b.createdAt;
        r.updatedAt     = b.updatedAt;
        return r;
    }
}

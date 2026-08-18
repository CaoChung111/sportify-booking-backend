package com.sportify.booking.service;

import com.sportify.booking.client.FieldServiceClient;
import com.sportify.booking.client.PaymentServiceClient;
import com.sportify.booking.dto.BookingDto;
import com.sportify.booking.entity.Booking;
import com.sportify.booking.resource.BookingSseResource;
import com.sportify.common.dto.PageResponse;
import com.sportify.common.exception.ServiceException;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class BookingService {

    @Inject
    @RestClient
    FieldServiceClient fieldServiceClient;

    @Inject
    @RestClient
    PaymentServiceClient paymentServiceClient;

    @Inject
    BookingSseResource bookingSseResource;

    // ── Check Availability ───────────────────────────────────────────────────

    public void checkSlotAvailability(Long fieldId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Optional<Booking> conflict = Booking.findFirstConflict(fieldId, date, startTime, endTime);
        if (conflict.isPresent()) {
            Booking existingBooking = conflict.get();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            String message = String.format(
                    "Khung giờ này đã được đặt hoặc đang được giữ để thanh toán (từ %s đến %s). Vui lòng thử lại sau ít phút.",
                    existingBooking.getStartTime().format(timeFormatter),
                    existingBooking.getEndTime().format(timeFormatter)
            );
            throw ServiceException.conflict(message);
        }
    }

    // ── Tạo Đơn Đặt Sân ──────────────────────────────────────────────────────

    @Transactional
    public BookingDto.BookingResponse create(Long userId, BookingDto.CreateBookingRequest request) {
        if (!request.endTime.isAfter(request.startTime)) {
            throw ServiceException.badRequest("endTime must be after startTime");
        }
        if (request.bookingDate == null) {
            throw ServiceException.badRequest("bookingDate is required");
        }
        if (request.bookingDate.isEqual(LocalDate.now()) && request.startTime.isBefore(LocalTime.now())) {
            throw ServiceException.badRequest("Cannot book a time slot that has already passed today");
        }

        var fieldResponse = fieldServiceClient.getField(request.fieldId);
        if (fieldResponse == null || fieldResponse.getData() == null) {
            throw ServiceException.notFound("Field", request.fieldId);
        }
        var fieldDetail = fieldResponse.getData();

        var availResp = fieldServiceClient.checkAvailability(request.fieldId);
        if (availResp == null || !Boolean.TRUE.equals(availResp.getData())) {
            throw ServiceException.badRequest(
                    "Field '" + fieldDetail.name() + "' is currently under maintenance and cannot be booked");
        }

        Booking.lockSlot(request.fieldId, request.bookingDate);
        checkSlotAvailability(request.fieldId, request.bookingDate, request.startTime, request.endTime);

        String dateStr  = request.bookingDate.toString();
        String startStr = request.startTime.toString();
        String endStr   = request.endTime.toString();
        var priceResp = fieldServiceClient.calculatePrice(request.fieldId, dateStr, startStr, endStr);
        if (priceResp == null || priceResp.getData() == null) {
            throw ServiceException.badRequest("Cannot calculate price for this field and time slot");
        }
        var priceDetail = priceResp.getData();

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

        BookingDto.BookingResponse response = toResponse(booking);

        bookingSseResource.pushSlotUpdate(
                booking.fieldId,
                booking.bookingDate,
                booking.startTime,
                booking.endTime,
                "PENDING"
        );
        bookingSseResource.pushNewBookingNotification(response);

        return response;
    }

    // ── Lấy Danh Sách Đặt Sân Của User ───────────────────────────────────────

    public List<BookingDto.BookingResponse> getMyBookings(Long userId) {
        return Booking.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<BookingDto.BookingResponse> getMyBookingsWithPagination(Long userId, String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        StringBuilder hql = new StringBuilder("userId = :userId");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);

        if (status != null && !status.isBlank()) {
            hql.append(" and status = :status");
            try {
                params.put("status", Booking.BookingStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw ServiceException.badRequest("Invalid status: " + status);
            }
        }

        var panacheQuery = Booking.find(hql.toString(), Sort.by("createdAt", Sort.Direction.Descending), params);
        long totalItems = panacheQuery.count();
        List<BookingDto.BookingResponse> items = panacheQuery
                .page(Page.of(safePage, safeSize))
                .<Booking>list()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(items, safePage, safeSize, totalItems, "createdAt", "desc");
    }

    public List<BookingDto.BookingResponse> getAllBookings() {
        return Booking.listAll().stream()
                .map(b -> toResponse((Booking) b))
                .sorted((b1, b2) -> b2.createdAt.compareTo(b1.createdAt))
                .collect(Collectors.toList());
    }

    public PageResponse<BookingDto.BookingResponse> getAllBookingsWithPagination(String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        StringBuilder hql = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (status != null && !status.isBlank()) {
            hql.append(" and status = :status");
            try {
                params.put("status", Booking.BookingStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw ServiceException.badRequest("Invalid status: " + status);
            }
        }

        var panacheQuery = Booking.find(hql.toString(), Sort.by("createdAt", Sort.Direction.Descending), params);
        long totalItems = panacheQuery.count();
        List<BookingDto.BookingResponse> items = panacheQuery
                .page(Page.of(safePage, safeSize))
                .<Booking>list()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(items, safePage, safeSize, totalItems, "createdAt", "desc");
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

        if (booking.status == Booking.BookingStatus.PAID_PENDING_CONFIRMATION ||
                booking.status == Booking.BookingStatus.CONFIRMED) {
            throw ServiceException.badRequest(
                    "Cannot cancel a paid booking. Please contact support for refund.");
        }

        if (booking.status == Booking.BookingStatus.CANCELLED) {
            throw ServiceException.badRequest("Booking is already cancelled");
        }

        if (booking.status == Booking.BookingStatus.COMPLETED) {
            throw ServiceException.badRequest("Cannot cancel a completed booking");
        }

        booking.status = Booking.BookingStatus.CANCELLED;
        booking.persist();
        cancelPaymentIfNeeded(booking.id);

        // SSE: Push slot-update event (slot vừa được giải phóng)
        bookingSseResource.pushSlotUpdate(
                booking.fieldId,
                booking.bookingDate,
                booking.startTime,
                booking.endTime,
                "CANCELLED"
        );
    }

    // ── Xác Nhận Booking (Internal — gọi bởi Payment Service) ────────────────

    /**
     * Xác nhận booking sau khi thanh toán thành công.
     * QUAN TRỌNG: Endpoint này chỉ được Payment Service gọi,
     * không phải user. Trong production cần bảo vệ bằng mTLS hoặc internal network.
     *
     * Luồng: Payment SUCCESS → Payment Service gọi API này → Booking = CONFIRMED
     */
    private void cancelPaymentIfNeeded(Long bookingId) {
        try {
            var paymentResponse = paymentServiceClient.getByBookingId(bookingId);
            if (paymentResponse == null || paymentResponse.getData() == null) {
                return;
            }

            var payment = paymentResponse.getData();
            if (!"SUCCESS".equalsIgnoreCase(payment.paymentStatus())) {
                paymentServiceClient.cancelByBookingId(bookingId);
            }
        } catch (Exception e) {
            throw ServiceException.badRequest("Cannot cancel payment for booking " + bookingId);
        }
    }

    @Transactional
    public void confirm(Long id) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);

        if (booking.status != Booking.BookingStatus.PENDING &&
                booking.status != Booking.BookingStatus.CASH_PENDING_PAYMENT &&
                booking.status != Booking.BookingStatus.PAID_PENDING_CONFIRMATION) {
            throw ServiceException.badRequest(
                    "Booking cannot be confirmed: current status is " + booking.status);
        }

        booking.status = Booking.BookingStatus.CONFIRMED;
        booking.persist();
        confirmPaymentIfNeeded(booking.id);
    }

    // ── Hoàn thành Booking (cập nhật sau khi chơi xong) ──────────────────────

    private void confirmPaymentIfNeeded(Long bookingId) {
        try {
            var paymentResponse = paymentServiceClient.getByBookingId(bookingId);
            if (paymentResponse == null || paymentResponse.getData() == null) {
                return;
            }

            var payment = paymentResponse.getData();
            if (!"SUCCESS".equalsIgnoreCase(payment.paymentStatus())) {
                paymentServiceClient.confirmByBookingId(bookingId);
            }
        } catch (Exception e) {
            throw ServiceException.badRequest("Cannot confirm payment for booking " + bookingId);
        }
    }

    @Transactional
    public void markPaidPendingConfirmation(Long id) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);

        if (booking.status == Booking.BookingStatus.PAID_PENDING_CONFIRMATION) {
            return;
        }

        if (booking.status != Booking.BookingStatus.PENDING) {
            throw ServiceException.badRequest(
                    "Booking cannot be marked as paid: current status is " + booking.status);
        }

        booking.status = Booking.BookingStatus.PAID_PENDING_CONFIRMATION;
        booking.persist();
    }

    @Transactional
    public void markCashPendingPayment(Long id) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);

        if (booking.status == Booking.BookingStatus.CASH_PENDING_PAYMENT) {
            return;
        }

        if (booking.status != Booking.BookingStatus.PENDING) {
            throw ServiceException.badRequest(
                    "Booking cannot be marked as cash pending payment: current status is " + booking.status);
        }

        booking.status = Booking.BookingStatus.CASH_PENDING_PAYMENT;
        booking.persist();
    }

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

package com.sportify.booking.service;

import com.sportify.booking.client.FieldServiceClient;
import com.sportify.booking.dto.BookingDto;
import com.sportify.booking.entity.Booking;
import com.sportify.common.exception.ServiceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class BookingService {

    @Inject
    @RestClient
    FieldServiceClient fieldServiceClient;

    /**
     * Tạo booking mới:
     * 1. Check field tồn tại và AVAILABLE
     * 2. Check slot chưa bị đặt (hasConflict)
     * 3. Lấy giá
     * 4. Tạo Booking status = PENDING
     */
    @Transactional
    public BookingDto.BookingResponse create(Long userId, BookingDto.CreateBookingRequest request) {
        // 1. Lấy thông tin field
        var fieldResponse = fieldServiceClient.getField(request.fieldId);
        if (fieldResponse == null || fieldResponse.getData() == null) {
            throw ServiceException.notFound("Field", request.fieldId);
        }
        var fieldDetail = fieldResponse.getData();

        if (!"AVAILABLE".equalsIgnoreCase(fieldDetail.status())) {
            throw ServiceException.badRequest("Field is not available for booking");
        }

        // 2. Check availability (conflict)
        String dateStr = request.bookingDate.toString();
        String startStr = request.startTime.toString();
        String endStr = request.endTime.toString();

        var availResp = fieldServiceClient.checkAvailability(request.fieldId, dateStr, startStr, endStr);
        boolean fieldAvailable = availResp != null && Boolean.TRUE.equals(availResp.getData());
        if (!fieldAvailable) {
            throw ServiceException.badRequest("Field is under maintenance or unavailable");
        }

        // Check conflict in booking DB
        if (Booking.hasConflict(request.fieldId, request.bookingDate, request.startTime, request.endTime)) {
            throw ServiceException.conflict("This time slot is already booked");
        }

        // 3. Lấy giá
        var priceResp = fieldServiceClient.calculatePrice(request.fieldId, dateStr, startStr, endStr);
        if (priceResp == null || priceResp.getData() == null) {
            throw ServiceException.badRequest("Cannot calculate price for this booking");
        }
        var priceDetail = priceResp.getData();

        // 4. Tạo booking
        Booking booking = new Booking();
        booking.userId = userId;
        booking.fieldId = request.fieldId;
        booking.fieldName = fieldDetail.name();
        booking.locationName = fieldDetail.locationName();
        booking.bookingDate = request.bookingDate;
        booking.startTime = request.startTime;
        booking.endTime = request.endTime;
        booking.totalPrice = priceDetail.totalPrice();
        booking.status = Booking.BookingStatus.PENDING;
        booking.persist();

        return toResponse(booking);
    }

    public List<BookingDto.BookingResponse> getMyBookings(Long userId) {
        return Booking.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BookingDto.BookingResponse getById(Long id, Long userId) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);
        if (!booking.userId.equals(userId)) throw ServiceException.badRequest("Access denied");
        return toResponse(booking);
    }

    @Transactional
    public void cancel(Long id, Long userId) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);
        if (!booking.userId.equals(userId)) throw ServiceException.badRequest("Access denied");
        if (booking.status == Booking.BookingStatus.CONFIRMED) {
            throw ServiceException.badRequest("Cannot cancel a confirmed booking");
        }
        booking.status = Booking.BookingStatus.CANCELLED;
        booking.persist();
    }

    /**
     * Confirm booking — được gọi bởi payment-service sau khi thanh toán thành công
     */
    @Transactional
    public void confirm(Long id) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);
        if (booking.status != Booking.BookingStatus.PENDING) {
            throw ServiceException.badRequest("Booking is not in PENDING state");
        }
        booking.status = Booking.BookingStatus.CONFIRMED;
        booking.persist();
    }

    /**
     * Internal: Get booking detail (called by payment-service)
     */
    public BookingDto.BookingResponse getByIdInternal(Long id) {
        Booking booking = Booking.findById(id);
        if (booking == null) throw ServiceException.notFound("Booking", id);
        return toResponse(booking);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BookingDto.BookingResponse toResponse(Booking b) {
        BookingDto.BookingResponse r = new BookingDto.BookingResponse();
        r.id = b.id;
        r.userId = b.userId;
        r.fieldId = b.fieldId;
        r.fieldName = b.fieldName;
        r.locationName = b.locationName;
        r.bookingDate = b.bookingDate;
        r.startTime = b.startTime;
        r.endTime = b.endTime;
        r.totalPrice = b.totalPrice;
        r.status = b.status.name();
        r.createdAt = b.createdAt;
        return r;
    }
}

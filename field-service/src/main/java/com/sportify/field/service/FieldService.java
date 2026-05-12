package com.sportify.field.service;

import com.sportify.common.exception.ServiceException;
import com.sportify.field.dto.FieldDto;
import com.sportify.field.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class FieldService {

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách sân, lọc theo location và/hoặc sport.
     */
    public List<FieldDto.FieldResponse> findAll(Long locationId, Long sportId) {
        List<Field> fields = (locationId != null)
                ? Field.findByLocation(locationId)
                : Field.<Field>listAll();

        return fields.stream()
                .filter(f -> sportId == null || f.fieldType.sport.id.equals(sportId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một sân theo ID.
     */
    public FieldDto.FieldResponse findById(Long id) {
        Field field = Field.findById(id);
        if (field == null) throw ServiceException.notFound("Field", id);
        return toResponse(field);
    }

    /**
     * Kiểm tra sân có trạng thái AVAILABLE không.
     * Đây là API nội bộ được Booking Service gọi.
     * Lưu ý: Chỉ kiểm tra trạng thái vận hành sân (AVAILABLE/MAINTENANCE).
     * Việc kiểm tra xung đột lịch cụ thể do Booking Service tự xử lý.
     */
    public boolean isAvailable(Long fieldId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Field field = Field.findById(fieldId);
        if (field == null) throw ServiceException.notFound("Field", fieldId);
        return field.status == Field.Status.AVAILABLE;
    }

    /**
     * Tính giá cho khung giờ đặt sân — nghiệp vụ Dynamic Pricing.
     *
     * Thuật toán:
     * 1. Xác định loại ngày (WEEKDAY / WEEKEND)
     * 2. Tìm Price Rule khớp: location + fieldType + startTime nằm trong [start_time, end_time) + dayType
     * 3. Fallback: nếu không có rule WEEKEND → dùng rule WEEKDAY
     * 4. Tính: totalPrice = pricePerHour × durationHours
     */
    public FieldDto.PriceResponse calculatePrice(Long fieldId, LocalDate date,
                                                  LocalTime startTime, LocalTime endTime) {
        Field field = Field.findById(fieldId);
        if (field == null) throw ServiceException.notFound("Field", fieldId);

        if (!endTime.isAfter(startTime)) {
            throw ServiceException.badRequest("endTime must be after startTime");
        }

        Price.DayType dayType    = resolveDayType(date);
        Long          locationId = field.location.id;
        Long          fieldTypeId = field.fieldType.id;

        // Tìm Price Rule khớp đúng loại ngày
        Price priceRule = Price.findApplicable(locationId, fieldTypeId, startTime, dayType);

        // Fallback: nếu WEEKEND/HOLIDAY không có rule → dùng WEEKDAY
        if (priceRule == null && dayType != Price.DayType.WEEKDAY) {
            priceRule = Price.findApplicable(locationId, fieldTypeId, startTime, Price.DayType.WEEKDAY);
        }

        if (priceRule == null) {
            throw ServiceException.badRequest(
                    "No price rule found for field " + fieldId +
                    " at " + startTime + " on " + dayType);
        }

        // Tính số giờ đặt
        long   durationMinutes = Duration.between(startTime, endTime).toMinutes();
        double hours           = durationMinutes / 60.0;
        BigDecimal totalPrice  = priceRule.price.multiply(BigDecimal.valueOf(hours));

        FieldDto.PriceResponse resp = new FieldDto.PriceResponse();
        resp.fieldId       = fieldId;
        resp.fieldName     = field.name;
        resp.pricePerHour  = priceRule.price;
        resp.durationHours = hours;
        resp.totalPrice    = totalPrice;
        resp.currency      = "VND";
        resp.dayType       = dayType.name();
        return resp;
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    /**
     * Tạo sân mới.
     * Admin phải cung cấp locationId và fieldTypeId hợp lệ.
     * Sân mới mặc định có trạng thái AVAILABLE.
     */
    @Transactional
    public FieldDto.FieldResponse create(FieldDto.CreateFieldRequest request) {
        Location location = Location.findById(request.locationId);
        if (location == null) throw ServiceException.notFound("Location", request.locationId);

        FieldType fieldType = FieldType.findById(request.fieldTypeId);
        if (fieldType == null) throw ServiceException.notFound("FieldType", request.fieldTypeId);

        // Kiểm tra trùng tên sân trong cùng địa điểm
        boolean exists = Field.find("name = ?1 and location.id = ?2", request.name, request.locationId)
                .firstResultOptional().isPresent();
        if (exists) {
            throw ServiceException.conflict("Field '" + request.name + "' already exists at this location");
        }

        Field field    = new Field();
        field.name     = request.name;
        field.location = location;
        field.fieldType = fieldType;
        field.status   = Field.Status.AVAILABLE;
        field.persist();

        return toResponse(field);
    }

    /**
     * Cập nhật thông tin sân.
     */
    @Transactional
    public FieldDto.FieldResponse update(Long id, FieldDto.CreateFieldRequest request) {
        Field field = Field.findById(id);
        if (field == null) throw ServiceException.notFound("Field", id);

        if (request.name != null) field.name = request.name;

        if (request.locationId != null) {
            Location location = Location.findById(request.locationId);
            if (location == null) throw ServiceException.notFound("Location", request.locationId);
            field.location = location;
        }

        if (request.fieldTypeId != null) {
            FieldType fieldType = FieldType.findById(request.fieldTypeId);
            if (fieldType == null) throw ServiceException.notFound("FieldType", request.fieldTypeId);
            field.fieldType = fieldType;
        }

        field.persist();
        return toResponse(field);
    }

    /**
     * Thay đổi trạng thái sân:
     * - AVAILABLE: Sân mở cửa, sẵn sàng nhận đặt lịch
     * - MAINTENANCE: Sân đang bảo trì, Booking Service sẽ từ chối đặt
     */
    @Transactional
    public void changeStatus(Long id, String status) {
        Field field = Field.findById(id);
        if (field == null) throw ServiceException.notFound("Field", id);

        try {
            field.status = Field.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ServiceException.badRequest(
                    "Invalid status: '" + status + "'. Must be AVAILABLE or MAINTENANCE");
        }

        field.persist();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Price.DayType resolveDayType(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return Price.DayType.WEEKEND;
        }
        return Price.DayType.WEEKDAY;
    }

    private FieldDto.FieldResponse toResponse(Field field) {
        FieldDto.FieldResponse r = new FieldDto.FieldResponse();
        r.id              = field.id;
        r.name            = field.name;
        r.status          = field.status.name();

        // Location
        r.locationId      = field.location.id;
        r.locationName    = field.location.name;
        r.locationAddress = field.location.address;
        r.locationRegion  = field.location.region;
        r.locationHotline = field.location.hotline;

        // FieldType
        r.fieldTypeId     = field.fieldType.id;
        r.fieldTypeName   = field.fieldType.name;
        r.playerCapacity  = field.fieldType.playerCapacity;

        // Sport
        r.sportId         = field.fieldType.sport.id;
        r.sportName       = field.fieldType.sport.name;
        r.sportSlug       = field.fieldType.sport.slug;

        return r;
    }
}

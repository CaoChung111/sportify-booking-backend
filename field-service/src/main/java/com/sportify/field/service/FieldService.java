package com.sportify.field.service;

import com.sportify.common.exception.ServiceException;
import com.sportify.field.dto.FieldDto;
import com.sportify.field.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class FieldService {

    // ── Queries ──────────────────────────────────────────────────────────────

    public List<FieldDto.FieldResponse> findAll(Long locationId, Long sportId) {
        List<Field> fields;
        if (locationId != null) {
            fields = Field.findByLocation(locationId);
        } else {
            fields = Field.listAll();
        }
        return fields.stream()
                .filter(f -> sportId == null || f.fieldType.sport.id.equals(sportId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public FieldDto.FieldResponse findById(Long id) {
        Field field = Field.findById(id);
        if (field == null) throw ServiceException.notFound("Field", id);
        return toResponse(field);
    }

    /**
     * Kiểm tra slot có trống không — delegates sang booking-service vẫn cần
     * nhưng ở field-service ta chỉ check trạng thái AVAILABLE của sân.
     * Conflict thực sự được booking-service check qua Booking.hasConflict().
     */
    public boolean isAvailable(Long fieldId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Field field = Field.findById(fieldId);
        if (field == null) throw ServiceException.notFound("Field", fieldId);
        return field.status == Field.Status.AVAILABLE;
    }

    /**
     * Tính giá dựa trên bảng giá (price table).
     * Nếu booking kéo dài qua nhiều khung giờ, cộng dồn từng khung.
     */
    public FieldDto.PriceResponse calculatePrice(Long fieldId, LocalDate date,
                                                  LocalTime startTime, LocalTime endTime) {
        Field field = Field.findById(fieldId);
        if (field == null) throw ServiceException.notFound("Field", fieldId);

        Price.DayType dayType = resolveDayType(date);
        Long locationId = field.location.id;
        Long fieldTypeId = field.fieldType.id;

        // Tìm price rule áp dụng cho startTime
        Price priceRule = Price.findApplicable(locationId, fieldTypeId, startTime, dayType);

        BigDecimal totalPrice;
        if (priceRule == null) {
            // Fallback: price rule WEEKDAY nếu không có WEEKEND
            priceRule = Price.findApplicable(locationId, fieldTypeId, startTime, Price.DayType.WEEKDAY);
        }
        if (priceRule == null) {
            throw ServiceException.badRequest("No price rule found for field " + fieldId + " at " + startTime);
        }

        // Tính số giờ đặt
        long durationMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        double hours = durationMinutes / 60.0;
        totalPrice = priceRule.price.multiply(BigDecimal.valueOf(hours));

        FieldDto.PriceResponse resp = new FieldDto.PriceResponse();
        resp.fieldId = fieldId;
        resp.totalPrice = totalPrice;
        resp.currency = "VND";
        return resp;
    }

    // ── Admin CRUD ───────────────────────────────────────────────────────────

    @Transactional
    public FieldDto.FieldResponse create(FieldDto.CreateFieldRequest request) {
        Location location = Location.findById(request.locationId);
        if (location == null) throw ServiceException.notFound("Location", request.locationId);

        FieldType fieldType = FieldType.findById(request.fieldTypeId);
        if (fieldType == null) throw ServiceException.notFound("FieldType", request.fieldTypeId);

        Field field = new Field();
        field.name = request.name;
        field.location = location;
        field.fieldType = fieldType;
        field.status = Field.Status.AVAILABLE;
        field.persist();

        return toResponse(field);
    }

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

    @Transactional
    public void changeStatus(Long id, String status) {
        Field field = Field.findById(id);
        if (field == null) throw ServiceException.notFound("Field", id);
        field.status = Field.Status.valueOf(status.toUpperCase());
        field.persist();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Price.DayType resolveDayType(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return Price.DayType.WEEKEND;
        }
        return Price.DayType.WEEKDAY;
    }

    private FieldDto.FieldResponse toResponse(Field field) {
        FieldDto.FieldResponse r = new FieldDto.FieldResponse();
        r.id = field.id;
        r.name = field.name;
        r.status = field.status.name();
        r.locationId = field.location.id;
        r.locationName = field.location.name;
        r.fieldTypeId = field.fieldType.id;
        r.fieldTypeName = field.fieldType.name;
        r.sportName = field.fieldType.sport.name;
        return r;
    }
}

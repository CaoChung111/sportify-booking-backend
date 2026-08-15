package com.sportify.field.service;

import com.sportify.common.exception.ServiceException;
import com.sportify.field.dto.FieldDto;
import com.sportify.field.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class FieldService {

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách sân, lọc theo tên, location và/hoặc sport.
     */
    public FieldDto.PageResponse<FieldDto.FieldResponse> findAll(
            String name, Long locationId, Long sportId, String status,
            int page, int size, String sortBy, String sortDir) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSortBy = resolveFieldSort(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.Descending
                : Sort.Direction.Ascending;

        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (name != null && !name.isBlank()) {
            query.append(" and lower(name) like :name");
            params.put("name", "%" + name.toLowerCase().trim() + "%");
        }
        if (locationId != null) {
            query.append(" and location.id = :locationId");
            params.put("locationId", locationId);
        }
        if (sportId != null) {
            query.append(" and fieldType.sport.id = :sportId");
            params.put("sportId", sportId);
        }
        if (status != null && !status.isBlank()) {
            query.append(" and status = :status");
            try {
                params.put("status", Field.Status.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw ServiceException.badRequest("Invalid status: '" + status + "'. Must be AVAILABLE or MAINTENANCE");
            }
        }

        var panacheQuery = Field.find(query.toString(), Sort.by(safeSortBy, direction), params);
        long totalItems = panacheQuery.count();
        List<FieldDto.FieldResponse> items = panacheQuery
                .page(Page.of(safePage, safeSize))
                .<Field>list()
                .stream()
                .map(this::toResponse)
                .toList();

        FieldDto.PageResponse<FieldDto.FieldResponse> response = new FieldDto.PageResponse<>();
        response.items = items;
        response.page = safePage;
        response.size = safeSize;
        response.totalItems = totalItems;
        response.totalPages = (int) Math.ceil((double) totalItems / safeSize);
        response.sortBy = safeSortBy;
        response.sortDir = direction == Sort.Direction.Descending ? "desc" : "asc";
        return response;
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
    public boolean isAvailable(Long fieldId) {
        Field field = Field.findById(fieldId);
        if (field == null) throw ServiceException.notFound("Field", fieldId);
        return field.status == Field.Status.AVAILABLE;
    }

    /**
     * Tính giá cho khung giờ đặt sân — nghiệp vụ Dynamic Pricing.
     * Thuật toán mới: Tính giá theo từng phân đoạn thời gian.
     */
    public FieldDto.PriceResponse calculatePrice(Long fieldId, LocalDate date,
                                                  LocalTime startTime, LocalTime endTime) {
        Field field = Field.findById(fieldId);
        if (field == null) throw ServiceException.notFound("Field", fieldId);

        if (!endTime.isAfter(startTime)) {
            throw ServiceException.badRequest("endTime must be after startTime");
        }

        Price.DayType dayType = resolveDayType(date);
        Long locationId = field.location.id;
        Long fieldTypeId = field.fieldType.id;

        // Lấy tất cả các quy tắc giá áp dụng cho loại sân và loại ngày này
        List<Price> priceRules = Price.findRulesForDay(locationId, fieldTypeId, dayType);

        // Fallback: nếu WEEKEND/HOLIDAY không có rule → dùng rule WEEKDAY
        if (priceRules.isEmpty() && dayType != Price.DayType.WEEKDAY) {
            priceRules = Price.findRulesForDay(locationId, fieldTypeId, Price.DayType.WEEKDAY);
        }

        if (priceRules.isEmpty()) {
            throw ServiceException.badRequest(
                    "No price rules found for field " + fieldId +
                    " on " + dayType + ". Please configure prices.");
        }

        BigDecimal totalCalculatedPrice = BigDecimal.ZERO;
        LocalTime currentSegmentStart = startTime;
        double totalDurationHours = 0.0;

        while (currentSegmentStart.isBefore(endTime)) {
            if (isLunchBreak(currentSegmentStart)) {
                currentSegmentStart = minTime(LocalTime.of(13, 0), endTime);
                continue;
            }

            // Tìm quy tắc giá áp dụng cho thời điểm hiện tại
            Price applicableRule = null;
            for (Price rule : priceRules) {
                if (!rule.getStartTime().isAfter(currentSegmentStart) && rule.getEndTime().isAfter(currentSegmentStart)) {
                    applicableRule = rule;
                    break;
                }
            }

            if (applicableRule == null) {
                throw ServiceException.badRequest(
                        "No price rule found for field " + fieldId +
                        " at " + currentSegmentStart + " on " + dayType + ". Please configure prices.");
            }

            // Xác định thời điểm kết thúc của phân đoạn hiện tại
            LocalTime segmentEnd = applicableRule.getEndTime();
            if (segmentEnd.isAfter(endTime)) {
                segmentEnd = endTime;
            }
            if (crossesLunchBreak(currentSegmentStart, segmentEnd)) {
                segmentEnd = LocalTime.of(12, 0);
            }

            // Tính thời lượng của phân đoạn
            long segmentMinutes = Duration.between(currentSegmentStart, segmentEnd).toMinutes();
            if (segmentMinutes <= 0) {
                currentSegmentStart = segmentEnd;
                continue;
            }
            BigDecimal segmentHours = BigDecimal.valueOf(segmentMinutes)
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

            // Tính giá cho phân đoạn và cộng vào tổng
            BigDecimal segmentPrice = applicableRule.getPrice().multiply(segmentHours);
            totalCalculatedPrice = totalCalculatedPrice.add(segmentPrice);
            totalDurationHours += segmentMinutes / 60.0;

            // Di chuyển đến thời điểm bắt đầu của phân đoạn tiếp theo
            currentSegmentStart = segmentEnd;
        }

        if (totalDurationHours <= 0) {
            throw ServiceException.badRequest("Booking time must include billable time outside lunch break 12:00-13:00");
        }

        FieldDto.PriceResponse resp = new FieldDto.PriceResponse();
        resp.fieldId       = fieldId;
        resp.fieldName     = field.name; // field.name đã được tải thông qua toResponse
        resp.totalPrice    = totalCalculatedPrice.setScale(2, RoundingMode.HALF_UP);
        resp.pricePerHour  = null; // Không còn khái niệm pricePerHour chung
        resp.durationHours = totalDurationHours;
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
        field.imageUrl = request.imageUrl;
        field.description = request.description;
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
        if (request.imageUrl != null) field.imageUrl = request.imageUrl;
        if (request.description != null) field.description = request.description;

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

    @Transactional
    public FieldDto.FieldResponse updateImage(Long id, String imageUrl) {
        Field field = Field.findById(id);
        if (field == null) throw ServiceException.notFound("Field", id);
        field.imageUrl = imageUrl;
        field.persist();
        return toResponse(field);
    }

    /**
     * Xóa sân.
     * Quy tắc an toàn:
     *  - Sân phải tồn tại.
     *  - Sân phải ở trạng thái MAINTENANCE trước khi xóa.
     *    (Buộc Admin đặt sân sang MAINTENANCE → tránh xóa sân đang vận hành,
     *     Booking Service có thể đang nhận đặt lịch trên sân đó.)
     */
    @Transactional
    public void delete(Long id) {
        Field field = Field.findById(id);
        if (field == null) throw ServiceException.notFound("Field", id);

        if (field.status == Field.Status.AVAILABLE) {
            throw ServiceException.badRequest(
                    "Cannot delete field '" + field.name + "': field must be set to MAINTENANCE before deletion");
        }

        field.delete();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Price.DayType resolveDayType(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return Price.DayType.WEEKEND;
        }
        return Price.DayType.WEEKDAY;
    }

    private boolean isLunchBreak(LocalTime time) {
        return !time.isBefore(LocalTime.of(12, 0)) && time.isBefore(LocalTime.of(13, 0));
    }

    private boolean crossesLunchBreak(LocalTime startTime, LocalTime endTime) {
        return startTime.isBefore(LocalTime.of(12, 0)) && endTime.isAfter(LocalTime.of(12, 0));
    }

    private LocalTime minTime(LocalTime first, LocalTime second) {
        return first.isBefore(second) ? first : second;
    }

    private String resolveFieldSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "id";
        }
        return switch (sortBy) {
            case "id", "name", "status" -> sortBy;
            case "locationId" -> "location.id";
            case "fieldTypeId" -> "fieldType.id";
            case "sportId" -> "fieldType.sport.id";
            default -> throw ServiceException.badRequest(
                    "Invalid sortBy: '" + sortBy + "'. Allowed: id, name, status, locationId, fieldTypeId, sportId");
        };
    }

    private FieldDto.FieldResponse toResponse(Field field) {
        FieldDto.FieldResponse r = new FieldDto.FieldResponse();
        r.id              = field.id;
        r.name            = field.name;
        r.status          = field.status.name();
        r.imageUrl        = field.imageUrl;
        r.description     = field.description;

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

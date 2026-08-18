package com.sportify.field.service;

import com.sportify.common.dto.PageResponse;
import com.sportify.common.exception.ServiceException;
import com.sportify.field.dto.PriceDto;
import com.sportify.field.entity.FieldType;
import com.sportify.field.entity.Location;
import com.sportify.field.entity.Price;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class PriceService {

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Lấy tất cả quy tắc giá, có thể lọc theo locationId và/hoặc fieldTypeId.
     */
    public List<PriceDto.PriceRuleResponse> findAll(Long locationId, Long fieldTypeId) {
        List<Price> prices;

        if (locationId != null && fieldTypeId != null) {
            prices = Price.list("location.id = ?1 AND fieldType.id = ?2", locationId, fieldTypeId);
        } else if (locationId != null) {
            prices = Price.list("location.id", locationId);
        } else if (fieldTypeId != null) {
            prices = Price.list("fieldType.id", fieldTypeId);
        } else {
            prices = Price.listAll();
        }

        return prices.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<PriceDto.PriceRuleResponse> findWithPagination(Long locationId, Long fieldTypeId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        StringBuilder hql = new StringBuilder("1 = 1");
        Map<String, Object> params = new HashMap<>();

        if (locationId != null) {
            hql.append(" and location.id = :locationId");
            params.put("locationId", locationId);
        }
        if (fieldTypeId != null) {
            hql.append(" and fieldType.id = :fieldTypeId");
            params.put("fieldTypeId", fieldTypeId);
        }

        var panacheQuery = Price.find(hql.toString(), Sort.by("id", Sort.Direction.Ascending), params);
        long totalItems = panacheQuery.count();
        List<PriceDto.PriceRuleResponse> items = panacheQuery
                .page(Page.of(safePage, safeSize))
                .<Price>list()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(items, safePage, safeSize, totalItems, "id", "asc");
    }

    public PriceDto.PriceRuleResponse findById(Long id) {
        Price price = Price.findById(id);
        if (price == null) throw ServiceException.notFound("Price rule", id);
        return toResponse(price);
    }

    public List<PriceDto.PriceRuleResponse> findPriceTable(Long locationId, Long fieldTypeId) {
        if (locationId == null) {
            throw ServiceException.badRequest("locationId is required");
        }
        if (fieldTypeId == null) {
            throw ServiceException.badRequest("fieldTypeId is required");
        }

        return Price.<Price>list(
                        "location.id = ?1 AND fieldType.id = ?2 ORDER BY dayType ASC, startTime ASC",
                        locationId, fieldTypeId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    /**
     * Tạo quy tắc giá mới.
     * Nghiệp vụ: Kiểm tra không bị overlap (trùng khung giờ) với quy tắc giá đã có
     * cho cùng location + fieldType + dayType.
     */
    @Transactional
    public PriceDto.PriceRuleResponse create(PriceDto.CreatePriceRequest request) {
        Location  location  = Location.findById(request.locationId);
        if (location == null) throw ServiceException.notFound("Location", request.locationId);

        FieldType fieldType = FieldType.findById(request.fieldTypeId);
        if (fieldType == null) throw ServiceException.notFound("FieldType", request.fieldTypeId);

        LocalTime startTime = parseTime(request.startTime, "startTime");
        LocalTime endTime   = parseTime(request.endTime, "endTime");

        if (!endTime.isAfter(startTime)) {
            throw ServiceException.badRequest("endTime must be after startTime");
        }

        Price.DayType dayType = parseDayType(request.dayType);

        // Kiểm tra overlap với các rule hiện có
        checkNoOverlap(request.locationId, request.fieldTypeId, startTime, endTime, dayType, null);

        Price price     = new Price();
        price.location  = location;
        price.fieldType = fieldType;
        price.startTime = startTime;
        price.endTime   = endTime;
        price.price     = request.price;
        price.dayType   = dayType;
        price.persist();

        return toResponse(price);
    }

    /**
     * Cập nhật quy tắc giá.
     * Vẫn kiểm tra overlap, loại trừ chính rule đang sửa.
     */
    @Transactional
    public PriceDto.PriceRuleResponse update(Long id, PriceDto.CreatePriceRequest request) {
        Price price = Price.findById(id);
        if (price == null) throw ServiceException.notFound("Price rule", id);

        if (request.locationId != null) {
            Location location = Location.findById(request.locationId);
            if (location == null) throw ServiceException.notFound("Location", request.locationId);
            price.location = location;
        }
        if (request.fieldTypeId != null) {
            FieldType fieldType = FieldType.findById(request.fieldTypeId);
            if (fieldType == null) throw ServiceException.notFound("FieldType", request.fieldTypeId);
            price.fieldType = fieldType;
        }
        if (request.startTime != null) price.startTime = parseTime(request.startTime, "startTime");
        if (request.endTime   != null) price.endTime   = parseTime(request.endTime, "endTime");
        if (request.price     != null) price.price     = request.price;
        if (request.dayType   != null) price.dayType   = parseDayType(request.dayType);

        if (!price.endTime.isAfter(price.startTime)) {
            throw ServiceException.badRequest("endTime must be after startTime");
        }

        // Kiểm tra overlap, loại trừ rule hiện tại
        checkNoOverlap(price.location.id, price.fieldType.id,
                price.startTime, price.endTime, price.dayType, id);

        price.persist();
        return toResponse(price);
    }

    /**
     * Xóa quy tắc giá.
     */
    @Transactional
    public void delete(Long id) {
        Price price = Price.findById(id);
        if (price == null) throw ServiceException.notFound("Price rule", id);
        price.delete();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Kiểm tra không có rule nào bị overlap khung giờ.
     * Hai khung giờ [A, B) và [C, D) overlap nếu: A < D AND C < B
     */
    private void checkNoOverlap(Long locationId, Long fieldTypeId,
                                 LocalTime start, LocalTime end,
                                 Price.DayType dayType, Long excludeId) {

        List<Price> existing = Price.list(
                "location.id = ?1 AND fieldType.id = ?2 AND dayType = ?3",
                locationId, fieldTypeId, dayType);

        for (Price p : existing) {
            if (excludeId != null && p.id.equals(excludeId)) continue;

            // A < D AND C < B
            boolean overlaps = start.isBefore(p.endTime) && p.startTime.isBefore(end);
            if (overlaps) {
                throw ServiceException.conflict(
                        "Time slot " + start + "–" + end +
                        " overlaps with existing rule " + p.startTime + "–" + p.endTime +
                        " (" + dayType + ")");
            }
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw ServiceException.badRequest("Invalid time format for " + fieldName + ": '" + value + "'. Expected HH:mm");
        }
    }

    private Price.DayType parseDayType(String value) {
        try {
            return Price.DayType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ServiceException.badRequest("Invalid dayType: '" + value + "'. Must be WEEKDAY, WEEKEND, or HOLIDAY");
        }
    }

    private PriceDto.PriceRuleResponse toResponse(Price p) {
        PriceDto.PriceRuleResponse r = new PriceDto.PriceRuleResponse();
        r.id            = p.id;
        r.locationId    = p.location.id;
        r.locationName  = p.location.name;
        r.fieldTypeId   = p.fieldType.id;
        r.fieldTypeName = p.fieldType.name;
        r.startTime     = p.startTime.toString();
        r.endTime       = p.endTime.toString();
        r.price         = p.price;
        r.currency      = "VND";
        r.dayType       = p.dayType.name();
        return r;
    }
}

package com.sportify.field.service;

import com.sportify.common.exception.ServiceException;
import com.sportify.field.dto.LocationDto;
import com.sportify.field.entity.Field;
import com.sportify.field.entity.Location;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class LocationService {

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<LocationDto.LocationResponse> findAll() {
        return Location.<Location>listAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public LocationDto.LocationResponse findById(Long id) {
        Location location = Location.findById(id);
        if (location == null) throw ServiceException.notFound("Location", id);
        return toResponse(location);
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    /**
     * Tạo địa điểm mới.
     * Kiểm tra trùng tên trong cùng khu vực.
     */
    @Transactional
    public LocationDto.LocationResponse create(LocationDto.CreateLocationRequest request) {
        // Kiểm tra trùng tên + region
        boolean exists = Location.find("name = ?1 and region = ?2", request.name, request.region)
                .firstResultOptional().isPresent();
        if (exists) {
            throw ServiceException.conflict(
                    "Location '" + request.name + "' already exists in region '" + request.region + "'");
        }

        Location location = new Location();
        location.name    = request.name;
        location.address = request.address;
        location.region  = request.region;
        location.hotline = request.hotline;
        location.persist();

        return toResponse(location);
    }

    /**
     * Cập nhật thông tin địa điểm.
     */
    @Transactional
    public LocationDto.LocationResponse update(Long id, LocationDto.CreateLocationRequest request) {
        Location location = Location.findById(id);
        if (location == null) throw ServiceException.notFound("Location", id);

        if (request.name    != null) location.name    = request.name;
        if (request.address != null) location.address = request.address;
        if (request.region  != null) location.region  = request.region;
        if (request.hotline != null) location.hotline = request.hotline;

        location.persist();
        return toResponse(location);
    }

    /**
     * Xóa địa điểm — chỉ cho phép nếu không còn sân nào thuộc địa điểm này.
     */
    @Transactional
    public void delete(Long id) {
        Location location = Location.findById(id);
        if (location == null) throw ServiceException.notFound("Location", id);

        long fieldCount = Field.count("location.id", id);
        if (fieldCount > 0) {
            throw ServiceException.badRequest(
                    "Cannot delete location: " + fieldCount + " field(s) still exist at this location");
        }

        location.delete();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LocationDto.LocationResponse toResponse(Location loc) {
        LocationDto.LocationResponse r = new LocationDto.LocationResponse();
        r.id          = loc.id;
        r.name        = loc.name;
        r.address     = loc.address;
        r.region      = loc.region;
        r.hotline     = loc.hotline;
        r.totalFields = (int) Field.count("location.id", loc.id);
        return r;
    }
}

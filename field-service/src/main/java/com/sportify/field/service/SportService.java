package com.sportify.field.service;

import com.sportify.common.dto.PageResponse;
import com.sportify.common.exception.ServiceException;
import com.sportify.field.dto.SportDto;
import com.sportify.field.entity.FieldType;
import com.sportify.field.entity.Sport;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SportService {

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<SportDto.SportResponse> findAll() {
        return Sport.<Sport>listAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PageResponse<SportDto.SportResponse> findWithPagination(String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        var query = (keyword != null && !keyword.isBlank())
                ? Sport.find("lower(name) like ?1 or lower(slug) like ?1",
                Sort.by("id", Sort.Direction.Ascending), "%" + keyword.toLowerCase().trim() + "%")
                : Sport.findAll(Sort.by("id", Sort.Direction.Ascending));

        long totalItems = query.count();
        List<SportDto.SportResponse> items = query
                .page(Page.of(safePage, safeSize))
                .<Sport>list()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(items, safePage, safeSize, totalItems, "id", "asc");
    }

    public SportDto.SportResponse findById(Long id) {
        Sport sport = Sport.findById(id);
        if (sport == null) throw ServiceException.notFound("Sport", id);
        return toResponse(sport);
    }

    public SportDto.SportResponse findBySlug(String slug) {
        Sport sport = Sport.findBySlug(slug);
        if (sport == null) throw ServiceException.notFound("Sport with slug", 0L);
        return toResponse(sport);
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    /**
     * Tạo môn thể thao mới.
     * Kiểm tra trùng slug — slug phải là duy nhất toàn hệ thống.
     */
    @Transactional
    public SportDto.SportResponse create(SportDto.CreateSportRequest request) {
        if (Sport.findBySlug(request.slug) != null) {
            throw ServiceException.conflict("Sport with slug '" + request.slug + "' already exists");
        }

        Sport sport  = new Sport();
        sport.name   = request.name;
        sport.slug   = request.slug.toLowerCase();
        sport.persist();

        return toResponse(sport);
    }

    /**
     * Cập nhật thông tin môn thể thao.
     * Nếu thay đổi slug, kiểm tra không trùng slug mới.
     */
    @Transactional
    public SportDto.SportResponse update(Long id, SportDto.CreateSportRequest request) {
        Sport sport = Sport.findById(id);
        if (sport == null) throw ServiceException.notFound("Sport", id);

        if (request.slug != null && !request.slug.equals(sport.slug)) {
            if (Sport.findBySlug(request.slug) != null) {
                throw ServiceException.conflict("Slug '" + request.slug + "' is already taken");
            }
            sport.slug = request.slug.toLowerCase();
        }
        if (request.name != null) sport.name = request.name;

        sport.persist();
        return toResponse(sport);
    }

    /**
     * Xóa môn thể thao — chỉ cho phép nếu không còn FieldType nào thuộc môn này.
     */
    @Transactional
    public void delete(Long id) {
        Sport sport = Sport.findById(id);
        if (sport == null) throw ServiceException.notFound("Sport", id);

        long fieldTypeCount = FieldType.count("sport.id", id);
        if (fieldTypeCount > 0) {
            throw ServiceException.badRequest(
                    "Cannot delete sport: " + fieldTypeCount + " field type(s) belong to this sport");
        }

        sport.delete();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SportDto.SportResponse toResponse(Sport sport) {
        SportDto.SportResponse r = new SportDto.SportResponse();
        r.id   = sport.id;
        r.name = sport.name;
        r.slug = sport.slug;
        return r;
    }
}

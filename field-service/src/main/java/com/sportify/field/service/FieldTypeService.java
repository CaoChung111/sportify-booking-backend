package com.sportify.field.service;

import com.sportify.common.exception.ServiceException;
import com.sportify.field.dto.FieldTypeDto;
import com.sportify.field.entity.Field;
import com.sportify.field.entity.FieldType;
import com.sportify.field.entity.Sport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class FieldTypeService {

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Lấy tất cả FieldType, tuỳ chọn lọc theo sportId.
     */
    public List<FieldTypeDto.FieldTypeResponse> findAll(Long sportId) {
        List<FieldType> list = (sportId != null)
                ? FieldType.list("sport.id", sportId)
                : FieldType.<FieldType>listAll();

        return list.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một FieldType theo ID.
     */
    public FieldTypeDto.FieldTypeResponse findById(Long id) {
        FieldType fieldType = FieldType.findById(id);
        if (fieldType == null) throw ServiceException.notFound("FieldType", id);
        return toResponse(fieldType);
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    /**
     * Tạo FieldType mới.
     * Quy tắc:
     *  - sportId phải tồn tại.
     *  - Tên FieldType phải là duy nhất trong cùng một môn thể thao.
     *  - playerCapacity >= 1.
     */
    @Transactional
    public FieldTypeDto.FieldTypeResponse create(FieldTypeDto.CreateFieldTypeRequest request) {
        Sport sport = Sport.findById(request.sportId);
        if (sport == null) throw ServiceException.notFound("Sport", request.sportId);

        // Kiểm tra trùng tên trong cùng sport
        boolean exists = FieldType
                .find("name = ?1 and sport.id = ?2", request.name, request.sportId)
                .firstResultOptional().isPresent();
        if (exists) {
            throw ServiceException.conflict(
                    "FieldType '" + request.name + "' already exists for sport id=" + request.sportId);
        }

        FieldType ft = new FieldType();
        ft.sport          = sport;
        ft.name           = request.name;
        ft.playerCapacity = request.playerCapacity;
        ft.persist();

        return toResponse(ft);
    }

    /**
     * Cập nhật FieldType.
     * Nếu thay đổi sport → kiểm tra sport mới tồn tại.
     * Nếu thay đổi tên → kiểm tra không trùng tên trong sport (sau khi xác định sport cuối cùng).
     */
    @Transactional
    public FieldTypeDto.FieldTypeResponse update(Long id, FieldTypeDto.CreateFieldTypeRequest request) {
        FieldType ft = FieldType.findById(id);
        if (ft == null) throw ServiceException.notFound("FieldType", id);

        // Xác định sport cuối cùng (sau khi thay đổi)
        Sport targetSport = ft.sport;
        if (request.sportId != null && !request.sportId.equals(ft.sport.id)) {
            targetSport = Sport.findById(request.sportId);
            if (targetSport == null) throw ServiceException.notFound("Sport", request.sportId);
            ft.sport = targetSport;
        }

        // Kiểm tra trùng tên trong sport (chỉ khi tên thay đổi hoặc sport thay đổi)
        if (request.name != null) {
            boolean nameConflict = FieldType
                    .find("name = ?1 and sport.id = ?2 and id <> ?3",
                            request.name, targetSport.id, id)
                    .firstResultOptional().isPresent();
            if (nameConflict) {
                throw ServiceException.conflict(
                        "FieldType '" + request.name + "' already exists for sport id=" + targetSport.id);
            }
            ft.name = request.name;
        }

        if (request.playerCapacity != null) {
            if (request.playerCapacity < 1) {
                throw ServiceException.badRequest("playerCapacity must be at least 1");
            }
            ft.playerCapacity = request.playerCapacity;
        }

        ft.persist();
        return toResponse(ft);
    }

    /**
     * Xóa FieldType.
     * Không được xóa nếu còn Field nào đang dùng FieldType này.
     */
    @Transactional
    public void delete(Long id) {
        FieldType ft = FieldType.findById(id);
        if (ft == null) throw ServiceException.notFound("FieldType", id);

        long fieldCount = Field.count("fieldType.id", id);
        if (fieldCount > 0) {
            throw ServiceException.badRequest(
                    "Cannot delete FieldType: " + fieldCount + " field(s) are still using this type");
        }

        ft.delete();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FieldTypeDto.FieldTypeResponse toResponse(FieldType ft) {
        FieldTypeDto.FieldTypeResponse r = new FieldTypeDto.FieldTypeResponse();
        r.id             = ft.id;
        r.name           = ft.name;
        r.playerCapacity = ft.playerCapacity;
        r.sportId        = ft.sport.id;
        r.sportName      = ft.sport.name;
        r.sportSlug      = ft.sport.slug;
        return r;
    }
}

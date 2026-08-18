package com.sportify.field.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.common.dto.PageResponse;
import com.sportify.field.dto.FieldTypeDto;
import com.sportify.field.service.FieldTypeService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/field-types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "FieldTypes", description = "Quản lý loại sân (FieldType)")
public class FieldTypeResource {

    @Inject
    FieldTypeService fieldTypeService;

    /**
     * GET /api/v1/field-types
     * Lấy danh sách tất cả FieldType, tuỳ chọn lọc theo sportId, keyword và phân trang.
     */
    @GET
    @PermitAll
    @Operation(summary = "Lấy danh sách FieldType (Public, lọc theo sportId, keyword, hỗ trợ phân trang)")
    public Response getAll(
            @Parameter(description = "Lọc theo ID môn thể thao")
            @QueryParam("sportId") Long sportId,
            @QueryParam("keyword") String keyword,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {

        if (page != null || size != null || (keyword != null && !keyword.isBlank())) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 10;
            PageResponse<FieldTypeDto.FieldTypeResponse> paginated = fieldTypeService.findWithPagination(sportId, keyword, p, s);
            return Response.ok(ApiResponse.success(paginated)).build();
        }

        List<FieldTypeDto.FieldTypeResponse> list = fieldTypeService.findAll(sportId);
        return Response.ok(ApiResponse.success(list)).build();
    }

    /**
     * GET /api/v1/field-types/{id}
     * Lấy chi tiết một FieldType theo ID.
     */
    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Lấy chi tiết FieldType theo ID")
    public Response getById(@PathParam("id") Long id) {
        FieldTypeDto.FieldTypeResponse ft = fieldTypeService.findById(id);
        return Response.ok(ApiResponse.success(ft)).build();
    }

    /**
     * POST /api/v1/field-types
     * Tạo FieldType mới — chỉ Admin.
     */
    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Tạo FieldType mới (Admin)")
    public Response create(@Valid FieldTypeDto.CreateFieldTypeRequest request) {
        FieldTypeDto.FieldTypeResponse created = fieldTypeService.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("FieldType created successfully", created))
                .build();
    }

    /**
     * PUT /api/v1/field-types/{id}
     * Cập nhật FieldType — chỉ Admin.
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Cập nhật FieldType (Admin)")
    public Response update(@PathParam("id") Long id,
                           @Valid FieldTypeDto.CreateFieldTypeRequest request) {
        FieldTypeDto.FieldTypeResponse updated = fieldTypeService.update(id, request);
        return Response.ok(ApiResponse.success("FieldType updated successfully", updated)).build();
    }

    /**
     * DELETE /api/v1/field-types/{id}
     * Xóa FieldType — chỉ Admin.
     * Trả về 409 nếu còn Field nào đang sử dụng FieldType này.
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Xóa FieldType (Admin) — chỉ được xóa nếu không còn Field nào dùng")
    public Response delete(@PathParam("id") Long id) {
        fieldTypeService.delete(id);
        return Response.ok(ApiResponse.success("FieldType deleted successfully", null)).build();
    }
}

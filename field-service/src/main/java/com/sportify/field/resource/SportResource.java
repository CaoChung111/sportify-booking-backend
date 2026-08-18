package com.sportify.field.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.common.dto.PageResponse;
import com.sportify.field.dto.SportDto;
import com.sportify.field.service.SportService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/sports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sports", description = "Quản lý môn thể thao")
public class SportResource {

    @Inject
    SportService sportService;

    /** GET /api/v1/sports — Lấy tất cả môn thể thao (hỗ trợ phân trang và lọc keyword) */
    @GET
    @PermitAll
    @Operation(summary = "Lấy danh sách môn thể thao (hỗ trợ phân trang và tìm kiếm)")
    public Response getAll(
            @QueryParam("keyword") String keyword,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {

        if (page != null || size != null || (keyword != null && !keyword.isBlank())) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 10;
            PageResponse<SportDto.SportResponse> paginated = sportService.findWithPagination(keyword, p, s);
            return Response.ok(ApiResponse.success(paginated)).build();
        }

        List<SportDto.SportResponse> list = sportService.findAll();
        return Response.ok(ApiResponse.success(list)).build();
    }

    /** GET /api/v1/sports/{id} — Chi tiết môn thể thao (Public) */
    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Lấy chi tiết môn thể thao theo ID")
    public Response getById(@PathParam("id") Long id) {
        SportDto.SportResponse sport = sportService.findById(id);
        return Response.ok(ApiResponse.success(sport)).build();
    }

    /** GET /api/v1/sports/slug/{slug} — Tìm theo slug (Public) */
    @GET
    @Path("/slug/{slug}")
    @PermitAll
    @Operation(summary = "Tìm môn thể thao theo slug (VD: bong-da)")
    public Response getBySlug(@PathParam("slug") String slug) {
        SportDto.SportResponse sport = sportService.findBySlug(slug);
        return Response.ok(ApiResponse.success(sport)).build();
    }

    /** POST /api/v1/sports — Tạo môn thể thao mới (Admin only) */
    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Tạo môn thể thao mới (Admin)")
    public Response create(@Valid SportDto.CreateSportRequest request) {
        SportDto.SportResponse created = sportService.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Sport created successfully", created))
                .build();
    }

    /** PUT /api/v1/sports/{id} — Cập nhật môn thể thao (Admin only) */
    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Cập nhật môn thể thao (Admin)")
    public Response update(@PathParam("id") Long id,
                           @Valid SportDto.CreateSportRequest request) {
        SportDto.SportResponse updated = sportService.update(id, request);
        return Response.ok(ApiResponse.success("Sport updated successfully", updated)).build();
    }

    /** DELETE /api/v1/sports/{id} — Xóa môn thể thao (Admin only) */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Xóa môn thể thao (Admin) — chỉ được xóa nếu không còn FieldType nào")
    public Response delete(@PathParam("id") Long id) {
        sportService.delete(id);
        return Response.ok(ApiResponse.success("Sport deleted successfully", null)).build();
    }
}

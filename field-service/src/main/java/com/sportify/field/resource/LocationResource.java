package com.sportify.field.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.field.dto.LocationDto;
import com.sportify.field.service.LocationService;
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

@Path("/api/v1/locations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Locations", description = "Quản lý địa điểm / cơ sở thể thao")
public class LocationResource {

    @Inject
    LocationService locationService;

    /** GET /api/v1/locations — Lấy danh sách tất cả địa điểm (Public) */
    @GET
    @PermitAll
    @Operation(summary = "Lấy danh sách tất cả địa điểm")
    public Response getAll() {
        List<LocationDto.LocationResponse> list = locationService.findAll();
        return Response.ok(ApiResponse.success(list)).build();
    }

    /** GET /api/v1/locations/{id} — Lấy chi tiết một địa điểm (Public) */
    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Lấy chi tiết địa điểm theo ID")
    public Response getById(@PathParam("id") Long id) {
        LocationDto.LocationResponse location = locationService.findById(id);
        return Response.ok(ApiResponse.success(location)).build();
    }

    /** POST /api/v1/locations — Tạo địa điểm mới (Admin only) */
    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Tạo địa điểm mới (Admin)")
    public Response create(@Valid LocationDto.CreateLocationRequest request) {
        LocationDto.LocationResponse created = locationService.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Location created successfully", created))
                .build();
    }

    /** PUT /api/v1/locations/{id} — Cập nhật địa điểm (Admin only) */
    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Cập nhật thông tin địa điểm (Admin)")
    public Response update(@PathParam("id") Long id,
                           @Valid LocationDto.CreateLocationRequest request) {
        LocationDto.LocationResponse updated = locationService.update(id, request);
        return Response.ok(ApiResponse.success("Location updated successfully", updated)).build();
    }

    /** DELETE /api/v1/locations/{id} — Xóa địa điểm (Admin only) */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Xóa địa điểm (Admin) — chỉ được xóa nếu không còn sân nào")
    public Response delete(@PathParam("id") Long id) {
        locationService.delete(id);
        return Response.ok(ApiResponse.success("Location deleted successfully", null)).build();
    }
}

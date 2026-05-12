package com.sportify.field.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.field.dto.PriceDto;
import com.sportify.field.service.PriceService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/prices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Prices", description = "Quản lý bảng giá sân thể thao (Admin)")
public class PriceResource {

    @Inject
    PriceService priceService;

    /**
     * GET /api/v1/prices
     * Lấy danh sách quy tắc giá, lọc theo locationId và/hoặc fieldTypeId.
     * Chỉ Admin mới xem được bảng giá chi tiết (khách hàng chỉ thấy kết quả
     * tính giá qua endpoint /fields/{id}/price).
     */
    @GET
    @RolesAllowed("ADMIN")
    @Operation(summary = "Lấy danh sách quy tắc giá (Admin)")
    public Response getAll(@QueryParam("locationId")  Long locationId,
                           @QueryParam("fieldTypeId") Long fieldTypeId) {
        List<PriceDto.PriceRuleResponse> list = priceService.findAll(locationId, fieldTypeId);
        return Response.ok(ApiResponse.success(list)).build();
    }

    /** GET /api/v1/prices/{id} — Chi tiết một quy tắc giá (Admin) */
    @GET
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Lấy chi tiết quy tắc giá theo ID (Admin)")
    public Response getById(@PathParam("id") Long id) {
        PriceDto.PriceRuleResponse price = priceService.findById(id);
        return Response.ok(ApiResponse.success(price)).build();
    }

    /**
     * POST /api/v1/prices
     * Tạo quy tắc giá mới.
     * Hệ thống kiểm tra không bị trùng lặp khung giờ (overlap)
     * cho cùng location + fieldType + dayType.
     */
    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Tạo quy tắc giá mới (Admin) — kiểm tra không overlap khung giờ")
    public Response create(@Valid PriceDto.CreatePriceRequest request) {
        PriceDto.PriceRuleResponse created = priceService.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Price rule created successfully", created))
                .build();
    }

    /**
     * PUT /api/v1/prices/{id}
     * Cập nhật quy tắc giá. Vẫn kiểm tra overlap sau khi sửa.
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Cập nhật quy tắc giá (Admin)")
    public Response update(@PathParam("id") Long id,
                           @Valid PriceDto.CreatePriceRequest request) {
        PriceDto.PriceRuleResponse updated = priceService.update(id, request);
        return Response.ok(ApiResponse.success("Price rule updated successfully", updated)).build();
    }

    /**
     * DELETE /api/v1/prices/{id}
     * Xóa quy tắc giá.
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Xóa quy tắc giá (Admin)")
    public Response delete(@PathParam("id") Long id) {
        priceService.delete(id);
        return Response.ok(ApiResponse.success("Price rule deleted successfully", null)).build();
    }
}

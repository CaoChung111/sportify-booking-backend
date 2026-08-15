package com.sportify.field.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.field.dto.FieldDto;
import com.sportify.field.service.CloudinaryService;
import com.sportify.field.service.FieldService;
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
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Path("/api/v1/fields")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Fields", description = "Quản lý sân thể thao và tra cứu giá")
public class FieldResource {

    @Inject
    FieldService fieldService;

    @Inject
    CloudinaryService cloudinaryService;

    /**
     * GET /api/v1/fields
     * Lấy danh sách sân, lọc tuỳ chọn theo tên, địa điểm và môn thể thao.
     */
    @GET
    @PermitAll
    @Operation(summary = "Lấy danh sách sân (lọc theo tên, location, sport)")
    public Response getAll(
            @Parameter(description = "Lọc theo tên sân (không phân biệt hoa thường)")
            @QueryParam("name") String name,
            @Parameter(description = "Lọc theo ID địa điểm")
            @QueryParam("locationId") Long locationId,
            @Parameter(description = "Lọc theo ID môn thể thao")
            @QueryParam("sportId") Long sportId,
            @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sortBy") @DefaultValue("id") String sortBy,
            @QueryParam("sortDir") @DefaultValue("asc") String sortDir) {

        FieldDto.PageResponse<FieldDto.FieldResponse> fields =
                fieldService.findAll(name, locationId, sportId, status, page, size, sortBy, sortDir);
        return Response.ok(ApiResponse.success(fields)).build();
    }

    /**
     * GET /api/v1/fields/{id}
     * Lấy chi tiết một sân theo ID.
     */
    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Lấy chi tiết sân theo ID")
    public Response getById(@PathParam("id") Long id) {
        FieldDto.FieldResponse field = fieldService.findById(id);
        return Response.ok(ApiResponse.success(field)).build();
    }

    /**
     * GET /api/v1/fields/{id}/availability
     * Kiểm tra sân có đang mở cửa không (AVAILABLE vs MAINTENANCE).
     * Được Booking Service gọi nội bộ.
     */
    @GET
    @Path("/{id}/availability")
    @PermitAll
    @Operation(summary = "Kiểm tra trạng thái vận hành của sân (AVAILABLE/MAINTENANCE)")
    public Response checkAvailability(@PathParam("id") Long fieldId) {
        boolean available = fieldService.isAvailable(fieldId);
        return Response.ok(ApiResponse.success(available)).build();
    }

    /**
     * GET /api/v1/fields/{id}/price?date=&startTime=&endTime=
     * Tính giá cho một khung giờ đặt sân cụ thể (Dynamic Pricing).
     * Được Booking Service gọi nội bộ trước khi tạo đơn.
     */
    @GET
    @Path("/{id}/price")
    @PermitAll
    @Operation(summary = "Tính giá đặt sân cho khung giờ cụ thể (Dynamic Pricing)")
    public Response calculatePrice(
            @PathParam("id") Long fieldId,
            @Parameter(description = "Ngày đặt sân (YYYY-MM-DD)", required = true)
            @QueryParam("date") String date,
            @Parameter(description = "Giờ bắt đầu (HH:mm)", required = true)
            @QueryParam("startTime") String startTime,
            @Parameter(description = "Giờ kết thúc (HH:mm)", required = true)
            @QueryParam("endTime") String endTime) {

        try {
            FieldDto.PriceResponse price = fieldService.calculatePrice(
                    fieldId,
                    LocalDate.parse(date),
                    LocalTime.parse(startTime),
                    LocalTime.parse(endTime));
            return Response.ok(ApiResponse.success(price)).build();
        } catch (DateTimeParseException e) {
            return Response.status(400)
                    .entity(ApiResponse.error("Invalid date/time format. Use YYYY-MM-DD and HH:mm"))
                    .build();
        }
    }

    /**
     * POST /api/v1/fields
     * Tạo sân mới — chỉ Admin.
     */
    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Tạo sân mới (Admin)")
    public Response create(@Valid FieldDto.CreateFieldRequest request) {
        FieldDto.FieldResponse created = fieldService.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Field created successfully", created))
                .build();
    }

    /**
     * PUT /api/v1/fields/{id}
     * Cập nhật thông tin sân — chỉ Admin.
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Cập nhật thông tin sân (Admin)")
    public Response update(@PathParam("id") Long id,
                           @Valid FieldDto.CreateFieldRequest request) {
        FieldDto.FieldResponse updated = fieldService.update(id, request);
        return Response.ok(ApiResponse.success("Field updated successfully", updated)).build();
    }

    @POST
    @Path("/{id}/image")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload áº£nh sÃ¢n lÃªn Cloudinary (Admin)")
    public Response uploadImage(@PathParam("id") Long id,
                                @RestForm("file") FileUpload file) {
        String imageUrl = cloudinaryService.uploadFieldImage(file);
        return Response.ok(ApiResponse.success("Image uploaded successfully", Map.of("imageUrl", imageUrl))).build();
    }

    /**
     * PATCH /api/v1/fields/{id}/status?status=AVAILABLE|MAINTENANCE
     * Đổi trạng thái sân — chỉ Admin.
     * MAINTENANCE: đóng sân, Booking Service sẽ từ chối đặt lịch.
     */
    @PATCH
    @Path("/{id}/status")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Thay đổi trạng thái sân: AVAILABLE hoặc MAINTENANCE (Admin)")
    public Response changeStatus(
            @PathParam("id") Long id,
            @Parameter(description = "Trạng thái mới: AVAILABLE hoặc MAINTENANCE", required = true)
            @QueryParam("status") String status) {

        if (status == null || status.isBlank()) {
            return Response.status(400)
                    .entity(ApiResponse.error("Query parameter 'status' is required"))
                    .build();
        }
        fieldService.changeStatus(id, status);
        return Response.ok(ApiResponse.success("Field status updated to " + status.toUpperCase(), null)).build();
    }

    /**
     * DELETE /api/v1/fields/{id}
     * Xóa sân — chỉ Admin.
     * Sân phải ở trạng thái MAINTENANCE trước khi xóa.
     * Trả về 400 nếu sân vẫn đang AVAILABLE.
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Xóa sân (Admin) — sân phải ở MAINTENANCE trước khi xóa")
    public Response delete(@PathParam("id") Long id) {
        fieldService.delete(id);
        return Response.ok(ApiResponse.success("Field deleted successfully", null)).build();
    }
}


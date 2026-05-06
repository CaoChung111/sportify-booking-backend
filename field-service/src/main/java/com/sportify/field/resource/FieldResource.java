package com.sportify.field.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.field.dto.FieldDto;
import com.sportify.field.service.FieldService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Path("/api/v1/fields")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Fields")
public class FieldResource {

    @Inject FieldService fieldService;

    @GET
    @PermitAll
    @Operation(summary = "Get all fields, optional filter by location")
    public Response getAll(@QueryParam("locationId") Long locationId,
                           @QueryParam("sportId") Long sportId) {
        List<FieldDto.FieldResponse> fields = fieldService.findAll(locationId, sportId);
        return Response.ok(ApiResponse.success(fields)).build();
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "Get field detail by ID")
    public Response getById(@PathParam("id") Long id) {
        FieldDto.FieldResponse field = fieldService.findById(id);
        return Response.ok(ApiResponse.success(field)).build();
    }

    @GET
    @Path("/{id}/availability")
    @PermitAll
    @Operation(summary = "Check field availability for a given date and time slot")
    public Response checkAvailability(@PathParam("id") Long fieldId,
                                      @QueryParam("date") String date,
                                      @QueryParam("startTime") String startTime,
                                      @QueryParam("endTime") String endTime) {
        boolean available = fieldService.isAvailable(fieldId,
                LocalDate.parse(date),
                LocalTime.parse(startTime),
                LocalTime.parse(endTime));
        return Response.ok(ApiResponse.success(available)).build();
    }

    @GET
    @Path("/{id}/price")
    @PermitAll
    @Operation(summary = "Calculate price for a booking slot")
    public Response calculatePrice(@PathParam("id") Long fieldId,
                                   @QueryParam("date") String date,
                                   @QueryParam("startTime") String startTime,
                                   @QueryParam("endTime") String endTime) {
        FieldDto.PriceResponse price = fieldService.calculatePrice(fieldId,
                LocalDate.parse(date),
                LocalTime.parse(startTime),
                LocalTime.parse(endTime));
        return Response.ok(ApiResponse.success(price)).build();
    }

    @POST
    @RolesAllowed("ADMIN")
    @Operation(summary = "Create a new field (Admin only)")
    public Response create(@Valid FieldDto.CreateFieldRequest request) {
        FieldDto.FieldResponse created = fieldService.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.success("Field created", created))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Update field (Admin only)")
    public Response update(@PathParam("id") Long id,
                           @Valid FieldDto.CreateFieldRequest request) {
        FieldDto.FieldResponse updated = fieldService.update(id, request);
        return Response.ok(ApiResponse.success("Field updated", updated)).build();
    }

    @PATCH
    @Path("/{id}/status")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Change field status (Admin only)")
    public Response changeStatus(@PathParam("id") Long id,
                                  @QueryParam("status") String status) {
        fieldService.changeStatus(id, status);
        return Response.ok(ApiResponse.success("Status updated", null)).build();
    }
}

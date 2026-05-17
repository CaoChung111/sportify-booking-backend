package com.sportify.gateway.resource;

import com.sportify.gateway.client.FieldServiceClient;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Field Gateway", description = "Proxy → field-service (port 8082)")
public class FieldGatewayResource {

    @Inject
    @RestClient
    FieldServiceClient fieldClient;

    @ConfigProperty(name = "quarkus.rest-client.field-service.url")
    String fieldServiceUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ── Fields ────────────────────────────────────────────────────────────────

    @GET
    @Path("/fields")
    @PermitAll
    @Operation(summary = "Lấy danh sách sân")
    public Response getFields(@QueryParam("name") String name,
                              @QueryParam("locationId") Long locationId,
                              @QueryParam("sportId") Long sportId,
                              @QueryParam("status") String status,
                              @QueryParam("page") Integer page,
                              @QueryParam("size") Integer size,
                              @QueryParam("sortBy") String sortBy,
                              @QueryParam("sortDir") String sortDir) {
        return fieldClient.getFields(name, locationId, sportId, status, page, size, sortBy, sortDir);
    }

    @GET
    @Path("/fields/{id}")
    @PermitAll
    @Operation(summary = "Lấy chi tiết sân theo ID")
    public Response getFieldById(@PathParam("id") Long id) {
        return fieldClient.getFieldById(id);
    }

    @GET
    @Path("/fields/{id}/availability")
    @PermitAll
    @Operation(summary = "Kiểm tra trạng thái vận hành của sân")
    public Response checkAvailability(@PathParam("id") Long id) {
        return fieldClient.checkAvailability(id);
    }

    @GET
    @Path("/fields/{id}/price")
    @PermitAll
    @Operation(summary = "Tính giá đặt sân cho khung giờ cụ thể")
    public Response calculatePrice(@PathParam("id") Long id,
                                   @QueryParam("date") String date,
                                   @QueryParam("startTime") String startTime,
                                   @QueryParam("endTime") String endTime) {
        return fieldClient.calculatePrice(id, date, startTime, endTime);
    }

    @POST
    @Path("/fields")
    @Operation(summary = "Tạo sân mới (Admin)")
    public Response createField(@Context HttpHeaders headers, Object body) {
        return fieldClient.createField(headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @PUT
    @Path("/fields/{id}")
    @Operation(summary = "Cập nhật thông tin sân (Admin)")
    public Response updateField(@PathParam("id") Long id,
                                @Context HttpHeaders headers, Object body) {
        return fieldClient.updateField(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @POST
    @Path("/fields/{id}/image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload áº£nh sÃ¢n (Admin)")
    public Response uploadFieldImage(@PathParam("id") Long id,
                                     @Context HttpHeaders headers,
                                     @RestForm("file") FileUpload file) {
        try {
            String boundary = "----SportifyGatewayBoundary" + UUID.randomUUID();
            byte[] body = multipartBody(boundary, file);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fieldServiceUrl + "/api/v1/upload/image"))
                    .header(HttpHeaders.AUTHORIZATION, headers.getHeaderString(HttpHeaders.AUTHORIZATION))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA + "; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return Response.status(response.statusCode())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(response.body())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"success\":false,\"message\":\"Upload failed: " + e.getMessage() + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @POST
    @Path("/upload/image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload áº£nh sÃ¢n (Admin)")
    public Response uploadImage(@Context HttpHeaders headers,
                                @RestForm("file") FileUpload file) {
        try {
            String boundary = "----SportifyGatewayBoundary" + UUID.randomUUID();
            byte[] body = multipartBody(boundary, file);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fieldServiceUrl + "/api/v1/upload/image"))
                    .header(HttpHeaders.AUTHORIZATION, headers.getHeaderString(HttpHeaders.AUTHORIZATION))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA + "; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return Response.status(response.statusCode())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(response.body())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"success\":false,\"message\":\"Upload failed: " + e.getMessage() + "\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @PATCH
    @Path("/fields/{id}/status")
    @Operation(summary = "Thay đổi trạng thái sân (Admin)")
    public Response changeFieldStatus(@PathParam("id") Long id,
                                      @Context HttpHeaders headers,
                                      @QueryParam("status") String status) {
        return fieldClient.changeFieldStatus(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION), status);
    }

    @DELETE
    @Path("/fields/{id}")
    @Operation(summary = "Xóa sân (Admin)")
    public Response deleteField(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return fieldClient.deleteField(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    // ── FieldTypes ────────────────────────────────────────────────────────────

    @GET
    @Path("/field-types")
    @PermitAll
    @Operation(summary = "Lấy danh sách loại sân")
    public Response getFieldTypes(@QueryParam("sportId") Long sportId) {
        return fieldClient.getFieldTypes(sportId);
    }

    @GET
    @Path("/field-types/{id}")
    @PermitAll
    @Operation(summary = "Lấy chi tiết loại sân theo ID")
    public Response getFieldTypeById(@PathParam("id") Long id) {
        return fieldClient.getFieldTypeById(id);
    }

    @POST
    @Path("/field-types")
    @Operation(summary = "Tạo loại sân mới (Admin)")
    public Response createFieldType(@Context HttpHeaders headers, Object body) {
        return fieldClient.createFieldType(headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @PUT
    @Path("/field-types/{id}")
    @Operation(summary = "Cập nhật thông tin loại sân (Admin)")
    public Response updateFieldType(@PathParam("id") Long id,
                                    @Context HttpHeaders headers, Object body) {
        return fieldClient.updateFieldType(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @DELETE
    @Path("/field-types/{id}")
    @Operation(summary = "Xóa loại sân (Admin)")
    public Response deleteFieldType(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return fieldClient.deleteFieldType(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    // ── Locations ─────────────────────────────────────────────────────────────

    @GET
    @Path("/locations")
    @PermitAll
    @Operation(summary = "Lấy danh sách địa điểm")
    public Response getLocations() {
        return fieldClient.getLocations();
    }

    @GET
    @Path("/locations/{id}")
    @PermitAll
    @Operation(summary = "Lấy chi tiết địa điểm theo ID")
    public Response getLocationById(@PathParam("id") Long id) {
        return fieldClient.getLocationById(id);
    }

    @POST
    @Path("/locations")
    @Operation(summary = "Tạo địa điểm mới (Admin)")
    public Response createLocation(@Context HttpHeaders headers, Object body) {
        return fieldClient.createLocation(headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @PUT
    @Path("/locations/{id}")
    @Operation(summary = "Cập nhật địa điểm (Admin)")
    public Response updateLocation(@PathParam("id") Long id,
                                   @Context HttpHeaders headers, Object body) {
        return fieldClient.updateLocation(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @DELETE
    @Path("/locations/{id}")
    @Operation(summary = "Xóa địa điểm (Admin)")
    public Response deleteLocation(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return fieldClient.deleteLocation(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    // ── Sports ────────────────────────────────────────────────────────────────

    @GET
    @Path("/sports")
    @PermitAll
    @Operation(summary = "Lấy danh sách môn thể thao")
    public Response getSports() {
        return fieldClient.getSports();
    }

    @GET
    @Path("/sports/{id}")
    @PermitAll
    @Operation(summary = "Lấy chi tiết môn thể thao theo ID")
    public Response getSportById(@PathParam("id") Long id) {
        return fieldClient.getSportById(id);
    }

    @GET
    @Path("/sports/slug/{slug}")
    @PermitAll
    @Operation(summary = "Tìm môn thể thao theo slug")
    public Response getSportBySlug(@PathParam("slug") String slug) {
        return fieldClient.getSportBySlug(slug);
    }

    @POST
    @Path("/sports")
    @Operation(summary = "Tạo môn thể thao mới (Admin)")
    public Response createSport(@Context HttpHeaders headers, Object body) {
        return fieldClient.createSport(headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @PUT
    @Path("/sports/{id}")
    @Operation(summary = "Cập nhật môn thể thao (Admin)")
    public Response updateSport(@PathParam("id") Long id,
                                @Context HttpHeaders headers, Object body) {
        return fieldClient.updateSport(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @DELETE
    @Path("/sports/{id}")
    @Operation(summary = "Xóa môn thể thao (Admin)")
    public Response deleteSport(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return fieldClient.deleteSport(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    // ── Prices ────────────────────────────────────────────────────────────────

    @GET
    @Path("/prices")
    @Operation(summary = "Lấy danh sách quy tắc giá (Admin)")
    public Response getPrices(@Context HttpHeaders headers,
                              @QueryParam("locationId") Long locationId,
                              @QueryParam("fieldTypeId") Long fieldTypeId) {
        return fieldClient.getPrices(headers.getHeaderString(HttpHeaders.AUTHORIZATION), locationId, fieldTypeId);
    }

    @GET
    @Path("/prices/table")
    @PermitAll
    @Operation(summary = "Lay bang gia theo locationId va fieldTypeId")
    public Response getPriceTable(@QueryParam("locationId") Long locationId,
                                  @QueryParam("fieldTypeId") Long fieldTypeId) {
        return fieldClient.getPriceTable(locationId, fieldTypeId);
    }

    @GET
    @Path("/prices/{id}")
    @Operation(summary = "Lấy chi tiết quy tắc giá (Admin)")
    public Response getPriceById(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return fieldClient.getPriceById(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    @POST
    @Path("/prices")
    @Operation(summary = "Tạo quy tắc giá mới (Admin)")
    public Response createPrice(@Context HttpHeaders headers, Object body) {
        return fieldClient.createPrice(headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @PUT
    @Path("/prices/{id}")
    @Operation(summary = "Cập nhật quy tắc giá (Admin)")
    public Response updatePrice(@PathParam("id") Long id,
                                @Context HttpHeaders headers, Object body) {
        return fieldClient.updatePrice(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION), body);
    }

    @DELETE
    @Path("/prices/{id}")
    @Operation(summary = "Xóa quy tắc giá (Admin)")
    public Response deletePrice(@PathParam("id") Long id, @Context HttpHeaders headers) {
        return fieldClient.deletePrice(id, headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    private byte[] multipartBody(String boundary, FileUpload file) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String fileName = file.fileName() != null ? file.fileName() : "field-image";
        String contentType = file.contentType() != null ? file.contentType() : "application/octet-stream";
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(Files.readAllBytes(file.uploadedFile()));
        output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }
}

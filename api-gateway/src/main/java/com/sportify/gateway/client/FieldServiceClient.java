package com.sportify.gateway.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "field-service")
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface FieldServiceClient {

    // ── Fields ────────────────────────────────────────────────────────────────

    @GET
    @Path("/fields")
    Response getFields(@QueryParam("name") String name,
                       @QueryParam("locationId") Long locationId,
                       @QueryParam("sportId") Long sportId);

    @GET
    @Path("/fields/{id}")
    Response getFieldById(@PathParam("id") Long id);

    @GET
    @Path("/fields/{id}/availability")
    Response checkAvailability(@PathParam("id") Long id);

    @GET
    @Path("/fields/{id}/price")
    Response calculatePrice(@PathParam("id") Long id,
                            @QueryParam("date") String date,
                            @QueryParam("startTime") String startTime,
                            @QueryParam("endTime") String endTime);

    @POST
    @Path("/fields")
    Response createField(@HeaderParam("Authorization") String authorization, Object body);

    @PUT
    @Path("/fields/{id}")
    Response updateField(@PathParam("id") Long id,
                         @HeaderParam("Authorization") String authorization,
                         Object body);

    @PATCH
    @Path("/fields/{id}/status")
    Response changeFieldStatus(@PathParam("id") Long id,
                               @HeaderParam("Authorization") String authorization,
                               @QueryParam("status") String status);

    // ── Locations ─────────────────────────────────────────────────────────────

    @GET
    @Path("/locations")
    Response getLocations();

    @GET
    @Path("/locations/{id}")
    Response getLocationById(@PathParam("id") Long id);

    @POST
    @Path("/locations")
    Response createLocation(@HeaderParam("Authorization") String authorization, Object body);

    @PUT
    @Path("/locations/{id}")
    Response updateLocation(@PathParam("id") Long id,
                            @HeaderParam("Authorization") String authorization,
                            Object body);

    @DELETE
    @Path("/locations/{id}")
    Response deleteLocation(@PathParam("id") Long id,
                            @HeaderParam("Authorization") String authorization);

    // ── Sports ────────────────────────────────────────────────────────────────

    @GET
    @Path("/sports")
    Response getSports();

    @GET
    @Path("/sports/{id}")
    Response getSportById(@PathParam("id") Long id);

    @GET
    @Path("/sports/slug/{slug}")
    Response getSportBySlug(@PathParam("slug") String slug);

    @POST
    @Path("/sports")
    Response createSport(@HeaderParam("Authorization") String authorization, Object body);

    @PUT
    @Path("/sports/{id}")
    Response updateSport(@PathParam("id") Long id,
                         @HeaderParam("Authorization") String authorization,
                         Object body);

    @DELETE
    @Path("/sports/{id}")
    Response deleteSport(@PathParam("id") Long id,
                         @HeaderParam("Authorization") String authorization);

    // ── Prices ────────────────────────────────────────────────────────────────

    @GET
    @Path("/prices")
    Response getPrices(@HeaderParam("Authorization") String authorization,
                       @QueryParam("locationId") Long locationId,
                       @QueryParam("fieldTypeId") Long fieldTypeId);

    @GET
    @Path("/prices/{id}")
    Response getPriceById(@PathParam("id") Long id,
                          @HeaderParam("Authorization") String authorization);

    @POST
    @Path("/prices")
    Response createPrice(@HeaderParam("Authorization") String authorization, Object body);

    @PUT
    @Path("/prices/{id}")
    Response updatePrice(@PathParam("id") Long id,
                         @HeaderParam("Authorization") String authorization,
                         Object body);

    @DELETE
    @Path("/prices/{id}")
    Response deletePrice(@PathParam("id") Long id,
                         @HeaderParam("Authorization") String authorization);
}

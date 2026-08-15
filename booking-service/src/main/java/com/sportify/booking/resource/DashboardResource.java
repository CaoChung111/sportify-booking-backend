package com.sportify.booking.resource;

import com.sportify.booking.dto.DashboardDto;
import com.sportify.booking.service.DashboardService;
import com.sportify.common.dto.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Path("/api/v1/dashboard/bookings")
@Tag(name = "Booking Dashboard", description = "Thong ke dat san cho admin")
public class DashboardResource {

    @Inject
    DashboardService dashboardService;

    @GET
    @RolesAllowed("ADMIN")
    @Operation(summary = "Tong hop dashboard booking")
    public Response getBookingDashboard(@QueryParam("from") LocalDate from,
                                        @QueryParam("to") LocalDate to,
                                        @QueryParam("topLimit") Integer topLimit) {
        DashboardDto.BookingDashboard dashboard = dashboardService.getBookingDashboard(from, to, topLimit);
        return Response.ok(ApiResponse.success("Success", dashboard)).type(MediaType.APPLICATION_JSON).build();
    }

    @GET
    @Path("/overview")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Thong ke tong quan booking")
    public Response getOverview(@QueryParam("from") LocalDate from,
                                @QueryParam("to") LocalDate to) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        return Response.ok(ApiResponse.success("Success", dashboardService.getOverview(resolvedFrom, resolvedTo))).build();
    }

    @GET
    @Path("/daily")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Thong ke booking theo ngay")
    public Response getDailyTrends(@QueryParam("from") LocalDate from,
                                   @QueryParam("to") LocalDate to) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        List<DashboardDto.DailyBookingTrend> trends = dashboardService.getDailyTrends(resolvedFrom, resolvedTo);
        return Response.ok(ApiResponse.success("Success", trends)).build();
    }

    @GET
    @Path("/top-fields")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Top san co nhieu booking")
    public Response getTopFields(@QueryParam("from") LocalDate from,
                                 @QueryParam("to") LocalDate to,
                                 @QueryParam("limit") Integer limit) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        int resolvedLimit = limit != null && limit > 0 ? Math.min(limit, 20) : 5;
        return Response.ok(ApiResponse.success("Success", dashboardService.getTopFields(resolvedFrom, resolvedTo, resolvedLimit))).build();
    }
}

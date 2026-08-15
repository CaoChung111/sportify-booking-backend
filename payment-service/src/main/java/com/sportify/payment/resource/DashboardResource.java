package com.sportify.payment.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.payment.dto.DashboardDto;
import com.sportify.payment.service.DashboardService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Path("/api/v1/dashboard/payments")
@Tag(name = "Payment Dashboard", description = "Thong ke thanh toan va doanh thu cho admin")
public class DashboardResource {

    @Inject
    DashboardService dashboardService;

    @GET
    @RolesAllowed("ADMIN")
    @Operation(summary = "Tong hop dashboard thanh toan")
    public Response getPaymentDashboard(@QueryParam("from") LocalDate from,
                                        @QueryParam("to") LocalDate to) {
        DashboardDto.PaymentDashboard dashboard = dashboardService.getPaymentDashboard(from, to);
        return Response.ok(ApiResponse.success("Success", dashboard)).build();
    }

    @GET
    @Path("/overview")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Thong ke tong quan thanh toan")
    public Response getOverview(@QueryParam("from") LocalDate from,
                                @QueryParam("to") LocalDate to) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        return Response.ok(ApiResponse.success("Success", dashboardService.getOverview(resolvedFrom, resolvedTo))).build();
    }

    @GET
    @Path("/daily-revenue")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Doanh thu thanh toan thanh cong theo ngay")
    public Response getDailyRevenue(@QueryParam("from") LocalDate from,
                                    @QueryParam("to") LocalDate to) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        List<DashboardDto.DailyRevenue> revenue = dashboardService.getDailyRevenue(resolvedFrom, resolvedTo);
        return Response.ok(ApiResponse.success("Success", revenue)).build();
    }
}

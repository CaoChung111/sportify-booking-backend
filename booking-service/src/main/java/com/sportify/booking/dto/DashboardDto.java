package com.sportify.booking.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DashboardDto {

    @Data
    public static class BookingOverview {
        public long totalBookings;
        public long todayBookings;
        public long upcomingBookings;
        public long pendingAdminConfirmation;
        public long cashPendingPayment;
        public long cancelledBookings;
        public BigDecimal expectedRevenue;
        public BigDecimal actualRevenue;
        public Map<String, Long> bookingsByStatus;
    }

    @Data
    public static class DailyBookingTrend {
        public LocalDate date;
        public long bookingCount;
        public BigDecimal expectedRevenue;
        public BigDecimal actualRevenue;
    }

    @Data
    public static class TopField {
        public Long fieldId;
        public String fieldName;
        public String locationName;
        public long bookingCount;
        public BigDecimal expectedRevenue;
        public BigDecimal actualRevenue;
    }

    @Data
    public static class BookingDashboard {
        public BookingOverview overview;
        public List<DailyBookingTrend> dailyTrends;
        public List<TopField> topFields;
    }
}

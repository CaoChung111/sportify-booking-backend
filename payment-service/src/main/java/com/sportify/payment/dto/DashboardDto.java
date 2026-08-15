package com.sportify.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DashboardDto {

    @Data
    public static class PaymentOverview {
        public long totalPayments;
        public long successfulPayments;
        public long pendingPayments;
        public long failedPayments;
        public long cancelledPayments;
        public BigDecimal totalRevenue;
        public BigDecimal todayRevenue;
        public BigDecimal averageSuccessfulPayment;
        public Map<String, Long> paymentsByStatus;
        public Map<String, Long> paymentsByMethod;
    }

    @Data
    public static class DailyRevenue {
        public LocalDate date;
        public BigDecimal revenue;
        public long successfulPaymentCount;
    }

    @Data
    public static class PaymentDashboard {
        public PaymentOverview overview;
        public List<DailyRevenue> dailyRevenue;
    }
}

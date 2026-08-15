package com.sportify.payment.service;

import com.sportify.payment.dto.DashboardDto;
import com.sportify.payment.entity.Payment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DashboardService {

    @PersistenceContext
    EntityManager entityManager;

    public DashboardDto.PaymentDashboard getPaymentDashboard(LocalDate from, LocalDate to) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();

        DashboardDto.PaymentDashboard dashboard = new DashboardDto.PaymentDashboard();
        dashboard.overview = getOverview(resolvedFrom, resolvedTo);
        dashboard.dailyRevenue = getDailyRevenue(resolvedFrom, resolvedTo);
        return dashboard;
    }

    public DashboardDto.PaymentOverview getOverview(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

        DashboardDto.PaymentOverview overview = new DashboardDto.PaymentOverview();
        overview.totalPayments = countBetween(fromDateTime, toDateTime);
        overview.successfulPayments = countByStatus(fromDateTime, toDateTime, Payment.PaymentStatus.SUCCESS);
        overview.pendingPayments = countByStatus(fromDateTime, toDateTime, Payment.PaymentStatus.PENDING);
        overview.failedPayments = countByStatus(fromDateTime, toDateTime, Payment.PaymentStatus.FAILED);
        overview.cancelledPayments = countByStatus(fromDateTime, toDateTime, Payment.PaymentStatus.CANCELLED);
        overview.totalRevenue = sumRevenue(fromDateTime, toDateTime);
        overview.todayRevenue = sumRevenue(LocalDate.now().atStartOfDay(), LocalDate.now().atTime(LocalTime.MAX));
        overview.averageSuccessfulPayment = overview.successfulPayments == 0
                ? BigDecimal.ZERO
                : overview.totalRevenue.divide(BigDecimal.valueOf(overview.successfulPayments), 2, RoundingMode.HALF_UP);
        overview.paymentsByStatus = paymentsByStatus(fromDateTime, toDateTime);
        overview.paymentsByMethod = paymentsByMethod(fromDateTime, toDateTime);
        return overview;
    }

    public List<DashboardDto.DailyRevenue> getDailyRevenue(LocalDate from, LocalDate to) {
        List<Object[]> rows = entityManager.createQuery("""
                select date(p.createdAt), coalesce(sum(p.amount), 0), count(p)
                from Payment p
                where p.createdAt between :from and :to
                  and p.paymentStatus = :status
                group by date(p.createdAt)
                order by date(p.createdAt) asc
                """, Object[].class)
                .setParameter("from", from.atStartOfDay())
                .setParameter("to", to.atTime(LocalTime.MAX))
                .setParameter("status", Payment.PaymentStatus.SUCCESS)
                .getResultList();

        Map<LocalDate, DashboardDto.DailyRevenue> byDate = new LinkedHashMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DashboardDto.DailyRevenue item = new DashboardDto.DailyRevenue();
            item.date = date;
            item.revenue = BigDecimal.ZERO;
            item.successfulPaymentCount = 0;
            byDate.put(date, item);
        }

        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            DashboardDto.DailyRevenue item = byDate.get(date);
            if (item != null) {
                item.revenue = toBigDecimal(row[1]);
                item.successfulPaymentCount = ((Number) row[2]).longValue();
            }
        }
        return new ArrayList<>(byDate.values());
    }

    private long countBetween(LocalDateTime from, LocalDateTime to) {
        return Payment.count("createdAt between ?1 and ?2", from, to);
    }

    private long countByStatus(LocalDateTime from, LocalDateTime to, Payment.PaymentStatus status) {
        return Payment.count("createdAt between ?1 and ?2 and paymentStatus = ?3", from, to, status);
    }

    private BigDecimal sumRevenue(LocalDateTime from, LocalDateTime to) {
        Object value = entityManager.createQuery("""
                select coalesce(sum(p.amount), 0)
                from Payment p
                where p.createdAt between :from and :to
                  and p.paymentStatus = :status
                """)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("status", Payment.PaymentStatus.SUCCESS)
                .getSingleResult();
        return toBigDecimal(value);
    }

    private Map<String, Long> paymentsByStatus(LocalDateTime from, LocalDateTime to) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Payment.PaymentStatus status : Payment.PaymentStatus.values()) {
            result.put(status.name(), 0L);
        }

        List<Object[]> rows = entityManager.createQuery("""
                select p.paymentStatus, count(p)
                from Payment p
                where p.createdAt between :from and :to
                group by p.paymentStatus
                """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        for (Object[] row : rows) {
            result.put(((Payment.PaymentStatus) row[0]).name(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private Map<String, Long> paymentsByMethod(LocalDateTime from, LocalDateTime to) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Payment.PaymentMethod method : Payment.PaymentMethod.values()) {
            result.put(method.name(), 0L);
        }

        List<Object[]> rows = entityManager.createQuery("""
                select p.paymentMethod, count(p)
                from Payment p
                where p.createdAt between :from and :to
                group by p.paymentMethod
                """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        for (Object[] row : rows) {
            result.put(((Payment.PaymentMethod) row[0]).name(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(value.toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }
}

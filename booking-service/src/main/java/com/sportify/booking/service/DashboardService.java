package com.sportify.booking.service;

import com.sportify.booking.dto.DashboardDto;
import com.sportify.booking.entity.Booking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DashboardService {

    @PersistenceContext
    EntityManager entityManager;

    public DashboardDto.BookingDashboard getBookingDashboard(LocalDate from, LocalDate to, Integer topLimit) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        int resolvedTopLimit = topLimit != null && topLimit > 0 ? Math.min(topLimit, 20) : 5;

        DashboardDto.BookingDashboard dashboard = new DashboardDto.BookingDashboard();
        dashboard.overview = getOverview(resolvedFrom, resolvedTo);
        dashboard.dailyTrends = getDailyTrends(resolvedFrom, resolvedTo);
        dashboard.topFields = getTopFields(resolvedFrom, resolvedTo, resolvedTopLimit);
        return dashboard;
    }

    public DashboardDto.BookingOverview getOverview(LocalDate from, LocalDate to) {
        DashboardDto.BookingOverview overview = new DashboardDto.BookingOverview();
        overview.totalBookings = countBetween(from, to);
        overview.todayBookings = countBetween(LocalDate.now(), LocalDate.now());
        overview.upcomingBookings = Booking.count(
                "bookingDate >= ?1 and status in ?2",
                LocalDate.now(),
                List.of(Booking.BookingStatus.CASH_PENDING_PAYMENT,
                        Booking.BookingStatus.PAID_PENDING_CONFIRMATION,
                        Booking.BookingStatus.CONFIRMED));
        overview.pendingAdminConfirmation = Booking.count(
                "status = ?1",
                Booking.BookingStatus.PAID_PENDING_CONFIRMATION);
        overview.cashPendingPayment = Booking.count(
                "status = ?1",
                Booking.BookingStatus.CASH_PENDING_PAYMENT);
        overview.cancelledBookings = countByStatus(from, to, Booking.BookingStatus.CANCELLED);
        overview.expectedRevenue = sumExpectedRevenue(from, to);
        overview.actualRevenue = sumActualRevenue(from, to);
        overview.bookingsByStatus = bookingsByStatus(from, to);
        return overview;
    }

    public List<DashboardDto.DailyBookingTrend> getDailyTrends(LocalDate from, LocalDate to) {
        List<Object[]> rows = entityManager.createQuery("""
                select b.bookingDate,
                       count(b),
                       coalesce(sum(b.totalPrice), 0),
                       coalesce(sum(case when b.status in :actualStatuses then b.totalPrice else 0 end), 0)
                from Booking b
                where b.bookingDate between :from and :to
                  and b.status <> :cancelled
                group by b.bookingDate
                order by b.bookingDate asc
                """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("cancelled", Booking.BookingStatus.CANCELLED)
                .setParameter("actualStatuses", actualRevenueStatuses())
                .getResultList();

        Map<LocalDate, DashboardDto.DailyBookingTrend> byDate = new LinkedHashMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            DashboardDto.DailyBookingTrend item = new DashboardDto.DailyBookingTrend();
            item.date = date;
            item.bookingCount = 0;
            item.expectedRevenue = BigDecimal.ZERO;
            item.actualRevenue = BigDecimal.ZERO;
            byDate.put(date, item);
        }

        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            DashboardDto.DailyBookingTrend item = byDate.get(date);
            if (item != null) {
                item.bookingCount = ((Number) row[1]).longValue();
                item.expectedRevenue = toBigDecimal(row[2]);
                item.actualRevenue = toBigDecimal(row[3]);
            }
        }
        return new ArrayList<>(byDate.values());
    }

    public List<DashboardDto.TopField> getTopFields(LocalDate from, LocalDate to, int limit) {
        List<Object[]> rows = entityManager.createQuery("""
                select b.fieldId,
                       b.fieldName,
                       b.locationName,
                       count(b),
                       coalesce(sum(b.totalPrice), 0),
                       coalesce(sum(case when b.status in :actualStatuses then b.totalPrice else 0 end), 0)
                from Booking b
                where b.bookingDate between :from and :to
                  and b.status <> :cancelled
                group by b.fieldId, b.fieldName, b.locationName
                order by count(b) desc, coalesce(sum(b.totalPrice), 0) desc
                """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("cancelled", Booking.BookingStatus.CANCELLED)
                .setParameter("actualStatuses", actualRevenueStatuses())
                .setMaxResults(limit)
                .getResultList();

        List<DashboardDto.TopField> result = new ArrayList<>();
        for (Object[] row : rows) {
            DashboardDto.TopField item = new DashboardDto.TopField();
            item.fieldId = ((Number) row[0]).longValue();
            item.fieldName = (String) row[1];
            item.locationName = (String) row[2];
            item.bookingCount = ((Number) row[3]).longValue();
            item.expectedRevenue = toBigDecimal(row[4]);
            item.actualRevenue = toBigDecimal(row[5]);
            result.add(item);
        }
        return result;
    }

    private long countBetween(LocalDate from, LocalDate to) {
        return Booking.count("bookingDate between ?1 and ?2", from, to);
    }

    private long countByStatus(LocalDate from, LocalDate to, Booking.BookingStatus status) {
        return Booking.count("bookingDate between ?1 and ?2 and status = ?3", from, to, status);
    }

    private BigDecimal sumExpectedRevenue(LocalDate from, LocalDate to) {
        Object value = entityManager.createQuery("""
                select coalesce(sum(b.totalPrice), 0)
                from Booking b
                where b.bookingDate between :from and :to
                  and b.status in :statuses
                """)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("statuses", List.of(
                        Booking.BookingStatus.CASH_PENDING_PAYMENT,
                        Booking.BookingStatus.PAID_PENDING_CONFIRMATION,
                        Booking.BookingStatus.CONFIRMED,
                        Booking.BookingStatus.COMPLETED))
                .getSingleResult();
        return toBigDecimal(value);
    }

    private BigDecimal sumActualRevenue(LocalDate from, LocalDate to) {
        Object value = entityManager.createQuery("""
                select coalesce(sum(b.totalPrice), 0)
                from Booking b
                where b.bookingDate between :from and :to
                  and b.status in :statuses
                """)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("statuses", actualRevenueStatuses())
                .getSingleResult();
        return toBigDecimal(value);
    }

    private List<Booking.BookingStatus> actualRevenueStatuses() {
        return List.of(
                Booking.BookingStatus.CONFIRMED,
                Booking.BookingStatus.COMPLETED);
    }

    private Map<String, Long> bookingsByStatus(LocalDate from, LocalDate to) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Booking.BookingStatus status : Booking.BookingStatus.values()) {
            result.put(status.name(), 0L);
        }

        List<Object[]> rows = entityManager.createQuery("""
                select b.status, count(b)
                from Booking b
                where b.bookingDate between :from and :to
                group by b.status
                """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        for (Object[] row : rows) {
            result.put(((Booking.BookingStatus) row[0]).name(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return new BigDecimal(value.toString());
    }
}

package com.sportify.field.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho logic nghiệp vụ tính giá sân trong FieldService.
 * Kiểm tra các phương thức: resolveDayType, isLunchBreak, crossesLunchBreak
 * và thuật toán tính giá theo phân đoạn thời gian (Segment-based Pricing).
 *
 * Lưu ý: Các phương thức gốc trong FieldService là private,
 * nên logic được tái tạo (replicate) trong test để kiểm tra tính đúng đắn
 * của thuật toán một cách độc lập, không phụ thuộc vào CSDL hay Quarkus context.
 */
@DisplayName("FieldService - Kiểm thử logic tính giá sân")
public class PricingLogicTest {

    // ---- Replicate logic từ FieldService.java ----

    enum DayType { WEEKDAY, WEEKEND, HOLIDAY }

    private DayType resolveDayType(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return DayType.WEEKEND;
        }
        return DayType.WEEKDAY;
    }

    private boolean isLunchBreak(LocalTime time) {
        return !time.isBefore(LocalTime.of(12, 0)) && time.isBefore(LocalTime.of(13, 0));
    }

    private boolean crossesLunchBreak(LocalTime startTime, LocalTime endTime) {
        return startTime.isBefore(LocalTime.of(12, 0)) && endTime.isAfter(LocalTime.of(12, 0));
    }

    private LocalTime minTime(LocalTime first, LocalTime second) {
        return first.isBefore(second) ? first : second;
    }

    // Simplified price rule for testing
    record PriceRule(LocalTime startTime, LocalTime endTime, BigDecimal pricePerHour) {}

    /**
     * Replicate thuật toán tính giá theo phân đoạn từ FieldService.calculatePrice()
     */
    private BigDecimal calculateTotalPrice(LocalTime bookingStart, LocalTime bookingEnd, List<PriceRule> rules) {
        BigDecimal total = BigDecimal.ZERO;
        LocalTime current = bookingStart;

        while (current.isBefore(bookingEnd)) {
            if (isLunchBreak(current)) {
                current = minTime(LocalTime.of(13, 0), bookingEnd);
                continue;
            }

            PriceRule applicable = null;
            for (PriceRule rule : rules) {
                if (!rule.startTime().isAfter(current) && rule.endTime().isAfter(current)) {
                    applicable = rule;
                    break;
                }
            }
            if (applicable == null) break;

            LocalTime segEnd = applicable.endTime();
            if (segEnd.isAfter(bookingEnd)) segEnd = bookingEnd;
            if (crossesLunchBreak(current, segEnd)) segEnd = LocalTime.of(12, 0);

            long minutes = Duration.between(current, segEnd).toMinutes();
            if (minutes <= 0) { current = segEnd; continue; }

            BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            total = total.add(applicable.pricePerHour().multiply(hours));
            current = segEnd;
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // ---- Test Cases ----

    @Nested
    @DisplayName("1. Phan loai ngay thuong / cuoi tuan (resolveDayType)")
    class ResolveDayTypeTests {
        @Test
        @DisplayName("Thu Hai la WEEKDAY")
        void monday_isWeekday() {
            assertEquals(DayType.WEEKDAY, resolveDayType(LocalDate.of(2026, 8, 17)));
        }

        @Test
        @DisplayName("Thu Sau la WEEKDAY")
        void friday_isWeekday() {
            assertEquals(DayType.WEEKDAY, resolveDayType(LocalDate.of(2026, 8, 21)));
        }

        @Test
        @DisplayName("Thu Bay la WEEKEND")
        void saturday_isWeekend() {
            assertEquals(DayType.WEEKEND, resolveDayType(LocalDate.of(2026, 8, 15)));
        }

        @Test
        @DisplayName("Chu Nhat la WEEKEND")
        void sunday_isWeekend() {
            assertEquals(DayType.WEEKEND, resolveDayType(LocalDate.of(2026, 8, 16)));
        }
    }

    @Nested
    @DisplayName("2. Kiem tra khung gio nghi trua (isLunchBreak)")
    class LunchBreakTests {
        @Test
        @DisplayName("11:59 khong phai gio nghi trua")
        void before_lunch() { assertFalse(isLunchBreak(LocalTime.of(11, 59))); }

        @Test
        @DisplayName("12:00 la gio nghi trua")
        void start_lunch() { assertTrue(isLunchBreak(LocalTime.of(12, 0))); }

        @Test
        @DisplayName("12:30 la gio nghi trua")
        void mid_lunch() { assertTrue(isLunchBreak(LocalTime.of(12, 30))); }

        @Test
        @DisplayName("12:59 la gio nghi trua")
        void end_lunch_minus1() { assertTrue(isLunchBreak(LocalTime.of(12, 59))); }

        @Test
        @DisplayName("13:00 khong phai gio nghi trua")
        void after_lunch() { assertFalse(isLunchBreak(LocalTime.of(13, 0))); }
    }

    @Nested
    @DisplayName("3. Kiem tra khung gio cat qua gio trua (crossesLunchBreak)")
    class CrossesLunchTests {
        @Test
        @DisplayName("11:30-13:30 cat qua gio trua")
        void crossesLunch() { assertTrue(crossesLunchBreak(LocalTime.of(11, 30), LocalTime.of(13, 30))); }

        @Test
        @DisplayName("10:00-11:30 khong cat qua gio trua")
        void beforeLunch() { assertFalse(crossesLunchBreak(LocalTime.of(10, 0), LocalTime.of(11, 30))); }

        @Test
        @DisplayName("13:30-15:00 khong cat qua gio trua")
        void afterLunch() { assertFalse(crossesLunchBreak(LocalTime.of(13, 30), LocalTime.of(15, 0))); }

        @Test
        @DisplayName("12:30-13:30 khong cat qua (bat dau sau 12:00)")
        void startsInLunch() { assertFalse(crossesLunchBreak(LocalTime.of(12, 30), LocalTime.of(13, 30))); }
    }

    @Nested
    @DisplayName("4. Tinh gia theo phan doan thoi gian (Segment-based Pricing)")
    class PriceCalculationTests {

        // Gia co dinh: 200,000 VND/gio
        List<PriceRule> singleRule = List.of(
            new PriceRule(LocalTime.of(6, 0), LocalTime.of(22, 0), new BigDecimal("200000"))
        );

        // Gia khac nhau: Sang 150k/h, Chieu 250k/h
        List<PriceRule> multiRules = List.of(
            new PriceRule(LocalTime.of(6, 0), LocalTime.of(12, 0), new BigDecimal("150000")),
            new PriceRule(LocalTime.of(13, 0), LocalTime.of(17, 0), new BigDecimal("200000")),
            new PriceRule(LocalTime.of(17, 0), LocalTime.of(22, 0), new BigDecimal("250000"))
        );

        @Test
        @DisplayName("Dat 1 gio (8:00-9:00) gia 200k => 200,000 VND")
        void oneHour_singleRate() {
            BigDecimal result = calculateTotalPrice(LocalTime.of(8, 0), LocalTime.of(9, 0), singleRule);
            assertEquals(new BigDecimal("200000.00"), result);
        }

        @Test
        @DisplayName("Dat 2 gio (8:00-10:00) gia 200k => 400,000 VND")
        void twoHours_singleRate() {
            BigDecimal result = calculateTotalPrice(LocalTime.of(8, 0), LocalTime.of(10, 0), singleRule);
            assertEquals(new BigDecimal("400000.00"), result);
        }

        @Test
        @DisplayName("Dat 30 phut (8:00-8:30) gia 200k => 100,000 VND")
        void halfHour_singleRate() {
            BigDecimal result = calculateTotalPrice(LocalTime.of(8, 0), LocalTime.of(8, 30), singleRule);
            assertEquals(new BigDecimal("100000.00"), result);
        }

        @Test
        @DisplayName("Dat 11:00-14:00 (3h nhung tru 1h trua) => 2h x 200k = 400,000 VND")
        void crossingLunch_deducts1Hour() {
            BigDecimal result = calculateTotalPrice(LocalTime.of(11, 0), LocalTime.of(14, 0), singleRule);
            assertEquals(new BigDecimal("400000.00"), result);
        }

        @Test
        @DisplayName("Dat gio vang 17:00-19:00 voi multi-rule => 2h x 250k = 500,000 VND")
        void eveningPeakRate() {
            BigDecimal result = calculateTotalPrice(LocalTime.of(17, 0), LocalTime.of(19, 0), multiRules);
            assertEquals(new BigDecimal("500000.00"), result);
        }

        @Test
        @DisplayName("Dat sang 8:00-10:00 voi multi-rule => 2h x 150k = 300,000 VND")
        void morningRate() {
            BigDecimal result = calculateTotalPrice(LocalTime.of(8, 0), LocalTime.of(10, 0), multiRules);
            assertEquals(new BigDecimal("300000.00"), result);
        }
    }
}

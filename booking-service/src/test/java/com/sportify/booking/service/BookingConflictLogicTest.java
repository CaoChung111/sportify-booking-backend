package com.sportify.booking.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("BookingService - Kiem thu logic phat hien trung lich dat san")
public class BookingConflictLogicTest {

    private boolean isOverlap(LocalTime newStart, LocalTime newEnd,
                              LocalTime existingStart, LocalTime existingEnd) {
        return newStart.isBefore(existingEnd) && existingStart.isBefore(newEnd);
    }

    @Nested
    @DisplayName("1. Truong hop KHONG xung dot (No Overlap)")
    class NoOverlapTests {

        @Test
        @DisplayName("Don moi ket thuc truoc don cu bat dau: [8:00-9:00] vs [10:00-11:00]")
        void newBooking_completelyBefore() {
            assertFalse(isOverlap(
                LocalTime.of(8, 0), LocalTime.of(9, 0),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        }

        @Test
        @DisplayName("Don moi bat dau sau don cu ket thuc: [12:00-13:00] vs [10:00-11:00]")
        void newBooking_completelyAfter() {
            assertFalse(isOverlap(
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        }

        @Test
        @DisplayName("Don moi ke lien truoc don cu (adjacent): [9:00-10:00] vs [10:00-11:00]")
        void newBooking_adjacentBefore() {
            assertFalse(isOverlap(
                LocalTime.of(9, 0), LocalTime.of(10, 0),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        }

        @Test
        @DisplayName("Don moi ke lien sau don cu (adjacent): [11:00-12:00] vs [10:00-11:00]")
        void newBooking_adjacentAfter() {
            assertFalse(isOverlap(
                LocalTime.of(11, 0), LocalTime.of(12, 0),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        }
    }

    @Nested
    @DisplayName("2. Truong hop CO xung dot (Overlap)")
    class OverlapTests {

        @Test
        @DisplayName("Don moi chong cheo nua dau don cu: [9:30-10:30] vs [10:00-11:00]")
        void partialOverlap_start() {
            assertTrue(isOverlap(
                LocalTime.of(9, 30), LocalTime.of(10, 30),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        }

        @Test
        @DisplayName("Don moi chong cheo nua cuoi don cu: [10:30-11:30] vs [10:00-11:00]")
        void partialOverlap_end() {
            assertTrue(isOverlap(
                LocalTime.of(10, 30), LocalTime.of(11, 30),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        }

        @Test
        @DisplayName("Don moi bao trum hoan toan don cu: [9:00-12:00] vs [10:00-11:00]")
        void newBooking_containsExisting() {
            assertTrue(isOverlap(
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        }

        @Test
        @DisplayName("Don cu bao trum hoan toan don moi: [10:15-10:45] vs [10:00-11:00]")
        void existingBooking_containsNew() {
            assertTrue(isOverlap(
                LocalTime.of(10, 15), LocalTime.of(10, 45),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        }

        @Test
        @DisplayName("Hai don trung khop hoan toan: [10:00-11:00] vs [10:00-11:00]")
        void exactSameSlot() {
            assertTrue(isOverlap(
                LocalTime.of(10, 0), LocalTime.of(11, 0),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        }
    }

    @Nested
    @DisplayName("3. Kiem tra cac truong hop dac biet")
    class EdgeCaseTests {

        @Test
        @DisplayName("Khung gio 15 phut chong cheo: [10:00-10:15] vs [10:00-11:00]")
        void fifteenMinuteOverlap() {
            assertTrue(isOverlap(
                LocalTime.of(10, 0), LocalTime.of(10, 15),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        }

        @Test
        @DisplayName("Dat san buoi toi ke nhau: [19:00-20:00] vs [20:00-21:00]")
        void eveningAdjacentSlots() {
            assertFalse(isOverlap(
                LocalTime.of(19, 0), LocalTime.of(20, 0),
                LocalTime.of(20, 0), LocalTime.of(21, 0)));
        }
    }
}

-- payment-service: V2__seed_payments.sql
-- Payment records tương ứng với các booking đã COMPLETED, CONFIRMED, PAID_PENDING_CONFIRMATION
-- Payment method: VNPAY (35 records), CASH (15 records)
-- Payment status: SUCCESS (cho COMPLETED/CONFIRMED), PENDING (cho PAID_PENDING_CONFIRMATION/booking PENDING)

INSERT INTO payments (booking_id, user_id, amount, payment_method, payment_status, txn_ref, created_at, updated_at) VALUES

-- ── Booking 1–10 (20–21/05 COMPLETED) ────────────────────────────────────────

-- booking_id=1: user=1, 150,000 — VNPAY SUCCESS
(1, 1, 150000.00, 'VNPAY', 'SUCCESS', '1_20260519200000', '2026-05-19 20:05:00', '2026-05-19 20:08:00'),

-- booking_id=2: user=2, 60,000 — CASH SUCCESS
(2, 2,  60000.00, 'CASH',  'SUCCESS', NULL,               '2026-05-19 21:05:00', '2026-05-20 08:30:00'),

-- booking_id=3: user=3, 260,000 — VNPAY SUCCESS
(3, 3, 260000.00, 'VNPAY', 'SUCCESS', '3_20260519180000', '2026-05-19 18:05:00', '2026-05-19 18:10:00'),

-- booking_id=4: user=4, 400,000 — CASH SUCCESS
(4, 4, 400000.00, 'CASH',  'SUCCESS', NULL,               '2026-05-20 08:05:00', '2026-05-20 19:30:00'),

-- booking_id=5: user=5, 90,000 — VNPAY SUCCESS
(5, 5,  90000.00, 'VNPAY', 'SUCCESS', '5_20260520220000', '2026-05-20 22:05:00', '2026-05-20 22:08:00'),

-- booking_id=6: user=1, 360,000 — VNPAY SUCCESS
(6, 1, 360000.00, 'VNPAY', 'SUCCESS', '6_20260520190000', '2026-05-20 19:05:00', '2026-05-20 19:09:00'),

-- booking_id=7: user=2, 160,000 — VNPAY CANCELLED (booking CANCELLED)
(7, 2, 160000.00, 'VNPAY', 'CANCELLED', '7_20260521070000', '2026-05-21 07:05:00', '2026-05-21 07:32:00'),

-- booking_id=8: user=3, 300,000 — VNPAY SUCCESS
(8, 3, 300000.00, 'VNPAY', 'SUCCESS', '8_20260521200000', '2026-05-21 20:05:00', '2026-05-21 20:09:00'),

-- booking_id=9: user=4, 82,500 — CASH SUCCESS
(9, 4,  82500.00, 'CASH',  'SUCCESS', NULL,               '2026-05-22 06:05:00', '2026-05-22 09:00:00'),

-- booking_id=10: user=5, 180,000 — VNPAY SUCCESS
(10, 5, 180000.00, 'VNPAY', 'SUCCESS', '10_20260521210000', '2026-05-21 21:05:00', '2026-05-21 21:09:00'),

-- ── Booking 11–19 (23–25/05 COMPLETED) ───────────────────────────────────────

-- booking_id=11: user=1, 260,000 — CASH SUCCESS
(11, 1, 260000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-22 20:05:00', '2026-05-23 09:30:00'),

-- booking_id=12: user=2, 380,000 — VNPAY SUCCESS
(12, 2, 380000.00, 'VNPAY', 'SUCCESS', '12_20260522180000', '2026-05-22 18:05:00', '2026-05-22 18:10:00'),

-- booking_id=13: user=3, 180,000 — VNPAY SUCCESS
(13, 3, 180000.00, 'VNPAY', 'SUCCESS', '13_20260523100000', '2026-05-23 10:05:00', '2026-05-23 10:09:00'),

-- booking_id=14: user=4, 320,000 — CASH SUCCESS
(14, 4, 320000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-22 22:05:00', '2026-05-23 18:30:00'),

-- booking_id=15: user=5, 420,000 — VNPAY SUCCESS
(15, 5, 420000.00, 'VNPAY', 'SUCCESS', '15_20260522210000', '2026-05-22 21:05:00', '2026-05-22 21:10:00'),

-- booking_id=16: user=1, 260,000 — VNPAY SUCCESS
(16, 1, 260000.00, 'VNPAY', 'SUCCESS', '16_20260523200000', '2026-05-23 20:05:00', '2026-05-23 20:09:00'),

-- booking_id=17: user=2, 200,000 — CASH SUCCESS
(17, 2, 200000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-23 22:05:00', '2026-05-24 10:30:00'),

-- booking_id=18: user=3, 127,500 — VNPAY SUCCESS
(18, 3, 127500.00, 'VNPAY', 'SUCCESS', '18_20260524080000', '2026-05-24 08:05:00', '2026-05-24 08:09:00'),

-- booking_id=19: user=4, 150,000 — CASH SUCCESS
(19, 4, 150000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-24 21:05:00', '2026-05-25 09:00:00'),

-- booking_id=20: user=5, 80,000 — VNPAY SUCCESS
(20, 5,  80000.00, 'VNPAY', 'SUCCESS', '20_20260525100000', '2026-05-25 10:05:00', '2026-05-25 10:09:00'),

-- ── Booking 21–28 (26–28/05 CONFIRMED) ───────────────────────────────────────

-- booking_id=21: user=1, 400,000 — CASH SUCCESS (CONFIRMED, chưa chơi)
(21, 1, 400000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-25 20:05:00', '2026-05-26 08:00:00'),

-- booking_id=22: user=2, 260,000 — VNPAY SUCCESS
(22, 2, 260000.00, 'VNPAY', 'SUCCESS', '22_20260525220000', '2026-05-25 22:05:00', '2026-05-25 22:10:00'),

-- booking_id=23: user=3, 70,000 — CASH SUCCESS
(23, 3,  70000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-26 06:05:00', '2026-05-26 07:00:00'),

-- booking_id=24: user=4, 200,000 — VNPAY SUCCESS
(24, 4, 200000.00, 'VNPAY', 'SUCCESS', '24_20260526200000', '2026-05-26 20:05:00', '2026-05-26 20:09:00'),

-- booking_id=25: user=5, 360,000 — VNPAY CANCELLED (booking CANCELLED)
(25, 5, 360000.00, 'VNPAY', 'CANCELLED', '25_20260527100000', '2026-05-27 10:05:00', '2026-05-27 10:32:00'),

-- booking_id=26: user=1, 75,000 — CASH SUCCESS
(26, 1,  75000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-27 08:05:00', '2026-05-27 09:00:00'),

-- booking_id=27: user=2, 200,000 — VNPAY SUCCESS (PAID_PENDING_CONFIRMATION)
(27, 2, 200000.00, 'VNPAY', 'SUCCESS', '27_20260527210000', '2026-05-27 21:05:00', '2026-05-27 21:10:00'),

-- booking_id=28: user=3, 80,000 — VNPAY SUCCESS (PAID_PENDING_CONFIRMATION)
(28, 3,  80000.00, 'VNPAY', 'SUCCESS', '28_20260528100000', '2026-05-28 10:05:00', '2026-05-28 10:09:00'),

-- booking_id=29: user=4, 400,000 — CASH SUCCESS (PAID_PENDING_CONFIRMATION)
(29, 4, 400000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-27 19:05:00', '2026-05-28 00:00:00'),

-- ── Booking 30–41 (29/05–31/05 CONFIRMED/PAID) ───────────────────────────────

-- booking_id=30: user=5, 260,000 — VNPAY SUCCESS
(30, 5, 260000.00, 'VNPAY', 'SUCCESS', '30_20260528220000', '2026-05-28 22:05:00', '2026-05-28 22:09:00'),

-- booking_id=31: user=1, 90,000 — CASH SUCCESS
(31, 1,  90000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-29 06:05:00', '2026-05-29 07:00:00'),

-- booking_id=32: user=2, 360,000 — VNPAY SUCCESS (PAID_PENDING_CONFIRMATION)
(32, 2, 360000.00, 'VNPAY', 'SUCCESS', '32_20260528200000', '2026-05-28 20:05:00', '2026-05-28 20:10:00'),

-- booking_id=33: user=3, 260,000 — VNPAY SUCCESS
(33, 3, 260000.00, 'VNPAY', 'SUCCESS', '33_20260529210000', '2026-05-29 21:05:00', '2026-05-29 21:09:00'),

-- booking_id=34: user=4, 260,000 — CASH SUCCESS
(34, 4, 260000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-29 20:05:00', '2026-05-30 07:30:00'),

-- booking_id=35: user=5, 200,000 — VNPAY SUCCESS
(35, 5, 200000.00, 'VNPAY', 'SUCCESS', '35_20260530100000', '2026-05-30 10:05:00', '2026-05-30 10:09:00'),

-- booking_id=36: user=1, 320,000 — CASH SUCCESS
(36, 1, 320000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-29 22:05:00', '2026-05-30 07:30:00'),

-- booking_id=37: user=2, 170,000 — VNPAY SUCCESS
(37, 2, 170000.00, 'VNPAY', 'SUCCESS', '37_20260529200000', '2026-05-29 20:05:00', '2026-05-29 20:09:00'),

-- booking_id=38: user=3, 480,000 — VNPAY SUCCESS
(38, 3, 480000.00, 'VNPAY', 'SUCCESS', '38_20260530180000', '2026-05-30 18:05:00', '2026-05-30 18:10:00'),

-- booking_id=39: user=4, 135,000 — CASH SUCCESS
(39, 4, 135000.00, 'CASH',  'SUCCESS', NULL,                '2026-05-31 07:05:00', '2026-05-31 07:30:00'),

-- booking_id=40: user=5, 420,000 — VNPAY SUCCESS (PAID_PENDING_CONFIRMATION)
(40, 5, 420000.00, 'VNPAY', 'SUCCESS', '40_20260530200000', '2026-05-30 20:05:00', '2026-05-30 20:10:00'),

-- ── Booking 41–50 (01/06–05/06 — CONFIRMED/PENDING) ─────────────────────────

-- booking_id=41: user=1, 150,000 — VNPAY SUCCESS
(41, 1, 150000.00, 'VNPAY', 'SUCCESS', '41_20260531210000', '2026-05-31 21:05:00', '2026-05-31 21:09:00'),

-- booking_id=42: user=2, 90,000 — CASH SUCCESS
(42, 2,  90000.00, 'CASH',  'SUCCESS', NULL,                '2026-06-01 10:05:00', '2026-06-01 11:00:00'),

-- booking_id=43–50: PENDING — chưa thanh toán (không có payment record)
-- Những booking từ 43-50 đang PENDING nên KHÔNG có payment record
-- (Người dùng chưa chọn thanh toán, hoặc đang trong quá trình thanh toán)

-- booking_id=43: 82,500 — PENDING (CASH, chưa xác nhận)
(43, 3, 82500.00, 'CASH', 'PENDING', NULL, '2026-06-01 06:05:00', NULL);

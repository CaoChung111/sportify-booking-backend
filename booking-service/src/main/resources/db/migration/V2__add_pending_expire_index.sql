-- booking-service: V2__add_pending_expire_index.sql
-- Thêm index compound (status, created_at) để Scheduler autoCancelExpiredBookings()
-- không phải full-scan toàn bộ bảng bookings mỗi phút.
--
-- Query của Scheduler:
--   SELECT * FROM bookings WHERE status = 'PENDING' AND created_at < :cutoff
-- → Index này cho phép MySQL locate ngay các row PENDING, sau đó filter theo created_at.

CREATE INDEX idx_bookings_status_created_at ON bookings (status, created_at);

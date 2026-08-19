ALTER TABLE notifications ADD COLUMN booking_id BIGINT;
CREATE INDEX idx_notifications_booking_id ON notifications(booking_id);

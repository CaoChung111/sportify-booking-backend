-- booking-service: V1__init_booking.sql
CREATE TABLE bookings (
                          id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                          user_id       BIGINT         NOT NULL,             -- ID reference, no FK (cross-service)
                          field_id      BIGINT         NOT NULL,             -- ID reference, no FK (cross-service)
                          field_name    VARCHAR(100)   NOT NULL,             -- snapshot tên sân tại thời điểm đặt
                          location_name VARCHAR(100)   NOT NULL,             -- snapshot tên địa điểm
                          booking_date  DATE           NOT NULL,
                          start_time    TIME           NOT NULL,
                          end_time      TIME           NOT NULL,
                          total_price   DECIMAL(15, 2) NOT NULL,
                          status        VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
                          note          TEXT,                                -- ghi chú của khách
                          version       INT            NOT NULL DEFAULT 0,  -- optimistic locking
                          created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at    DATETIME
);

-- Index hỗ trợ tìm kiếm booking theo user
CREATE INDEX idx_bookings_user_id    ON bookings(user_id);

-- Index hỗ trợ kiểm tra Double Booking: truy vấn theo fieldId + date
CREATE INDEX idx_bookings_field_date ON bookings(field_id, booking_date);

-- Index hỗ trợ lọc theo status
CREATE INDEX idx_bookings_status     ON bookings(status);

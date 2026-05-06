-- booking-service: V1__init_booking.sql
CREATE TABLE bookings (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT         NOT NULL,   -- ID reference, no FK
    field_id      BIGINT         NOT NULL,   -- ID reference, no FK
    field_name    VARCHAR(100)   NOT NULL,   -- snapshot
    location_name VARCHAR(100)   NOT NULL,   -- snapshot
    booking_date  DATE           NOT NULL,
    start_time    TIME           NOT NULL,
    end_time      TIME           NOT NULL,
    total_price   DECIMAL(15, 2) NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    version       INT            NOT NULL DEFAULT 0,
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_user_id  ON bookings(user_id);
CREATE INDEX idx_bookings_field_id ON bookings(field_id, booking_date);

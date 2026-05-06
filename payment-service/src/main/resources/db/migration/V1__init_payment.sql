-- payment-service: V1__init_payment.sql
CREATE TABLE payments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id     BIGINT         NOT NULL UNIQUE,  -- ID reference, no FK
    user_id        BIGINT         NOT NULL,          -- ID reference, no FK
    amount         DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(20)    NOT NULL,
    payment_status VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    txn_ref        VARCHAR(100)   UNIQUE,
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_user_id    ON payments(user_id);
CREATE INDEX idx_payments_txn_ref    ON payments(txn_ref);

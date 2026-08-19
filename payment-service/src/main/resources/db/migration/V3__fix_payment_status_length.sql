-- V3__fix_payment_status_length.sql
-- Fix: payment_status VARCHAR(20) quá ngắn, không chứa được 'PAID_PENDING_CONFIRMATION' (26 ký tự)
ALTER TABLE payments
    MODIFY COLUMN payment_method VARCHAR(50) NOT NULL,
    MODIFY COLUMN payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING';

-- ============================================================
-- SPORTIFY — Tạo Databases Rỗng
-- ============================================================
-- Chạy file này MỘT LẦN trước khi khởi động services.
-- Flyway sẽ tự động tạo bảng và seed data khi service khởi động.
--
-- Chạy: mysql -u root -p < init_databases.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS auth_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS field_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS booking_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS payment_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Kiểm tra kết quả
SHOW DATABASES LIKE '%db%';

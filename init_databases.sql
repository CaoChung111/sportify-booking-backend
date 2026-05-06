-- ─── Script tạo các Database và User cho Sportify Booking Microservices ───
-- Chạy script này bằng tài khoản root của MySQL:
-- mysql -u root -p < init_databases.sql

-- 1. Tạo các database
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS field_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS keycloak_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Tạo user cho các microservices (nếu chưa có)
-- Lưu ý: Trong môi trường dev, có thể dùng 1 user chung hoặc tách riêng.
-- Ở đây tạo user 'sportify' mật khẩu 'sportify123' cho các service nghiệp vụ.
CREATE USER IF NOT EXISTS 'sportify'@'%' IDENTIFIED BY 'sportify123';
CREATE USER IF NOT EXISTS 'sportify'@'localhost' IDENTIFIED BY 'sportify123';

-- 3. Cấp quyền cho user 'sportify' trên các database nghiệp vụ
GRANT ALL PRIVILEGES ON auth_db.* TO 'sportify'@'%';
GRANT ALL PRIVILEGES ON field_db.* TO 'sportify'@'%';
GRANT ALL PRIVILEGES ON booking_db.* TO 'sportify'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'sportify'@'%';

GRANT ALL PRIVILEGES ON auth_db.* TO 'sportify'@'localhost';
GRANT ALL PRIVILEGES ON field_db.* TO 'sportify'@'localhost';
GRANT ALL PRIVILEGES ON booking_db.* TO 'sportify'@'localhost';
GRANT ALL PRIVILEGES ON payment_db.* TO 'sportify'@'localhost';

-- 4. Tạo user riêng cho Keycloak
CREATE USER IF NOT EXISTS 'keycloak'@'%' IDENTIFIED BY 'keycloak123';
CREATE USER IF NOT EXISTS 'keycloak'@'localhost' IDENTIFIED BY 'keycloak123';

-- 5. Cấp quyền cho user 'keycloak'
GRANT ALL PRIVILEGES ON keycloak_db.* TO 'keycloak'@'%';
GRANT ALL PRIVILEGES ON keycloak_db.* TO 'keycloak'@'localhost';

-- 6. Áp dụng thay đổi quyền
FLUSH PRIVILEGES;

-- =========================================================================================
-- LƯU Ý: 
-- Các bảng (tables) sẽ được Quarkus tự động tạo thông qua Flyway migration khi bạn start 
-- từng service (nếu cấu hình quarkus.flyway.migrate-at-start=true như hiện tại).
-- Bạn CHỈ CẦN chạy script này ĐỂ TẠO DATABASE rỗng và TẠO USER.
-- =========================================================================================

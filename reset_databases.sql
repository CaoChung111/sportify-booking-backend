-- reset_databases.sql
-- Run this script to drop and recreate clean databases for Sportify microservices

DROP DATABASE IF EXISTS auth_db;
DROP DATABASE IF EXISTS field_db;
DROP DATABASE IF EXISTS booking_db;
DROP DATABASE IF EXISTS payment_db;

CREATE DATABASE auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE field_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE booking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON auth_db.* TO 'sportify'@'%';
GRANT ALL PRIVILEGES ON field_db.* TO 'sportify'@'%';
GRANT ALL PRIVILEGES ON booking_db.* TO 'sportify'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'sportify'@'%';

FLUSH PRIVILEGES;

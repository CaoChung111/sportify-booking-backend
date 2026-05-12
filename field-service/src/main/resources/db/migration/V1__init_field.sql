-- field-service: V1__init_field.sql
-- Schema + Seed data đầy đủ (gộp chung để 1 migration file duy nhất)

-- ── Schema ────────────────────────────────────────────────────────────────────

CREATE TABLE sports (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE locations (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    address TEXT         NOT NULL,
    region  VARCHAR(50)  NOT NULL,
    hotline VARCHAR(20)
);

CREATE TABLE field_types (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sport_id        BIGINT       NOT NULL,
    name            VARCHAR(100) NOT NULL,
    player_capacity INT          NOT NULL,
    FOREIGN KEY (sport_id) REFERENCES sports(id)
);

CREATE TABLE fields (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_id   BIGINT      NOT NULL,
    field_type_id BIGINT      NOT NULL,
    name          VARCHAR(50) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    FOREIGN KEY (location_id)   REFERENCES locations(id),
    FOREIGN KEY (field_type_id) REFERENCES field_types(id)
);

CREATE TABLE prices (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_id   BIGINT         NOT NULL,
    field_type_id BIGINT         NOT NULL,
    start_time    TIME           NOT NULL,
    end_time      TIME           NOT NULL,
    price         DECIMAL(15, 2) NOT NULL,
    day_type      VARCHAR(20)    NOT NULL,
    FOREIGN KEY (location_id)   REFERENCES locations(id),
    FOREIGN KEY (field_type_id) REFERENCES field_types(id)
);

CREATE INDEX idx_fields_location   ON fields(location_id);
CREATE INDEX idx_fields_field_type ON fields(field_type_id);
CREATE INDEX idx_prices_lookup     ON prices(location_id, field_type_id, day_type);

-- ── Seed Data ─────────────────────────────────────────────────────────────────

-- Môn thể thao
INSERT INTO sports (name, slug) VALUES
    ('Bóng đá',  'bong-da'),
    ('Cầu lông', 'cau-long'),
    ('Bóng rổ',  'bong-ro');

-- Địa điểm (location_id: 1=Hà Nội, 2=Bắc Ninh)
INSERT INTO locations (name, address, region, hotline) VALUES
    ('Sportify Hà Nội',  '123 Đường Láng, Đống Đa, Hà Nội',          'Hà Nội',  '0901234567'),
    ('Sportify Bắc Ninh','45 Đường Lý Thái Tổ, TP Bắc Ninh, Bắc Ninh','Bắc Ninh','0907654321');

-- Loại sân (sport_id: 1=Bóng đá, 2=Cầu lông, 3=Bóng rổ)
INSERT INTO field_types (sport_id, name, player_capacity) VALUES
    (1, 'Sân bóng đá 5 người',  10),   -- id=1
    (1, 'Sân bóng đá 7 người',  14),   -- id=2
    (1, 'Sân bóng đá 11 người', 22),   -- id=3
    (2, 'Sân cầu lông đơn',      2),   -- id=4
    (2, 'Sân cầu lông đôi',      4),   -- id=5
    (3, 'Sân bóng rổ 3x3',       6),   -- id=6
    (3, 'Sân bóng rổ 5x5',      10);   -- id=7

-- Sân — Sportify Hà Nội (location_id=1)
INSERT INTO fields (location_id, field_type_id, name, status) VALUES
    (1, 1, 'Sân A1 (5 người)', 'AVAILABLE'),
    (1, 1, 'Sân A2 (5 người)', 'AVAILABLE'),
    (1, 2, 'Sân B1 (7 người)', 'AVAILABLE'),
    (1, 4, 'Sân CL-01',        'AVAILABLE'),
    (1, 4, 'Sân CL-02',        'AVAILABLE'),
    (1, 5, 'Sân CL-03 (đôi)', 'AVAILABLE');

-- Sân — Sportify Bắc Ninh (location_id=2)
INSERT INTO fields (location_id, field_type_id, name, status) VALUES
    (2, 1, 'Sân A1 (5 người)', 'AVAILABLE'),
    (2, 2, 'Sân B1 (7 người)', 'AVAILABLE'),
    (2, 4, 'Sân CL-01',        'AVAILABLE');

-- Bảng giá — Hà Nội / Bóng đá 5 người
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 1, '06:00', '11:00', 100000, 'WEEKDAY'),
    (1, 1, '11:00', '16:00',  80000, 'WEEKDAY'),
    (1, 1, '16:00', '22:00', 150000, 'WEEKDAY'),
    (1, 1, '06:00', '11:00', 130000, 'WEEKEND'),
    (1, 1, '11:00', '16:00', 110000, 'WEEKEND'),
    (1, 1, '16:00', '22:00', 180000, 'WEEKEND');

-- Bảng giá — Hà Nội / Bóng đá 7 người
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 2, '06:00', '11:00', 150000, 'WEEKDAY'),
    (1, 2, '11:00', '16:00', 120000, 'WEEKDAY'),
    (1, 2, '16:00', '22:00', 200000, 'WEEKDAY'),
    (1, 2, '06:00', '11:00', 190000, 'WEEKEND'),
    (1, 2, '11:00', '16:00', 160000, 'WEEKEND'),
    (1, 2, '16:00', '22:00', 240000, 'WEEKEND');

-- Bảng giá — Hà Nội / Cầu lông đơn
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 4, '06:00', '11:00',  60000, 'WEEKDAY'),
    (1, 4, '11:00', '16:00',  50000, 'WEEKDAY'),
    (1, 4, '16:00', '22:00',  80000, 'WEEKDAY'),
    (1, 4, '06:00', '22:00',  90000, 'WEEKEND');

-- Bảng giá — Hà Nội / Cầu lông đôi
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 5, '06:00', '11:00',  70000, 'WEEKDAY'),
    (1, 5, '11:00', '16:00',  60000, 'WEEKDAY'),
    (1, 5, '16:00', '22:00',  90000, 'WEEKDAY'),
    (1, 5, '06:00', '22:00', 100000, 'WEEKEND');

-- Bảng giá — Bắc Ninh / Bóng đá 5 người
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (2, 1, '06:00', '11:00',  90000, 'WEEKDAY'),
    (2, 1, '11:00', '16:00',  75000, 'WEEKDAY'),
    (2, 1, '16:00', '22:00', 130000, 'WEEKDAY'),
    (2, 1, '06:00', '22:00', 160000, 'WEEKEND');

-- Bảng giá — Bắc Ninh / Bóng đá 7 người
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (2, 2, '06:00', '11:00', 130000, 'WEEKDAY'),
    (2, 2, '11:00', '16:00', 110000, 'WEEKDAY'),
    (2, 2, '16:00', '22:00', 180000, 'WEEKDAY'),
    (2, 2, '06:00', '22:00', 210000, 'WEEKEND');

-- Bảng giá — Bắc Ninh / Cầu lông đơn
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (2, 4, '06:00', '11:00',  55000, 'WEEKDAY'),
    (2, 4, '11:00', '16:00',  45000, 'WEEKDAY'),
    (2, 4, '16:00', '22:00',  75000, 'WEEKDAY'),
    (2, 4, '06:00', '22:00',  85000, 'WEEKEND');

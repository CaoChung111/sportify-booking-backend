-- field-service: V1__init_field.sql
-- Schema + Seed data đồng bộ hoàn toàn với field_db dump thực tế
-- Sports: 6 | Locations: 3 | FieldTypes: 11 | Fields: 12 | Prices: 66

-- ── Schema ────────────────────────────────────────────────────────────────────

CREATE TABLE sports (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50)  NOT NULL,
    slug VARCHAR(50)  NOT NULL,
    UNIQUE KEY UK_sports_slug (slug)
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
    location_id   BIGINT       NOT NULL,
    field_type_id BIGINT       NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    image_url     VARCHAR(500) DEFAULT NULL,
    description   TEXT,
    status        ENUM('AVAILABLE','MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    FOREIGN KEY (location_id)   REFERENCES locations(id),
    FOREIGN KEY (field_type_id) REFERENCES field_types(id)
);

CREATE TABLE prices (
    id            BIGINT         AUTO_INCREMENT PRIMARY KEY,
    location_id   BIGINT         NOT NULL,
    field_type_id BIGINT         NOT NULL,
    start_time    TIME           NOT NULL,
    end_time      TIME           NOT NULL,
    price         DECIMAL(15, 2) NOT NULL,
    day_type      ENUM('WEEKDAY','WEEKEND','HOLIDAY') NOT NULL,
    FOREIGN KEY (location_id)   REFERENCES locations(id),
    FOREIGN KEY (field_type_id) REFERENCES field_types(id)
);

CREATE INDEX idx_fields_location   ON fields(location_id);
CREATE INDEX idx_fields_field_type ON fields(field_type_id);
CREATE INDEX idx_prices_lookup     ON prices(location_id, field_type_id, day_type);

-- ── Seed Data ─────────────────────────────────────────────────────────────────

-- Môn thể thao (id: 1-6)
INSERT INTO sports (name, slug) VALUES
    ('Bóng đá',     'bong-da'),
    ('Cầu lông',    'cau-long'),
    ('Bóng rổ',     'bong-ro'),
    ('Pickleball',  'pickleball'),
    ('Bóng chuyền', 'bong-chuyen'),
    ('Đá cầu',      'da-cau');

-- Địa điểm (id: 1-3)
INSERT INTO locations (name, address, region, hotline) VALUES
    ('Sportify Hà Nội cơ sở 1',              '123 Minh Khai, Bắc Từ Liêm, Hà Nội', 'Hà Nội', '0907654321'),
    ('Sportify Hà Nội cở sở 2 (Đang thi công)', '456 Minh Khai, Bắc Từ Liêm, Hà Nội', 'Hà Nội', '0901234567'),
    ('Sportify Hà Nội cở sở 3 (Đang thi công)', '789 Minh Khai, Bắc Từ Liêm, Hà Nội', 'Hà Nội', '0907654322');

-- Loại sân (id: 1-11)
INSERT INTO field_types (sport_id, name, player_capacity) VALUES
    (1, 'Sân 5 người - Cỏ nhân tạo',       5),   -- id=1
    (1, 'Sân 7 người - Cỏ nhân tạo',       7),   -- id=2
    (1, 'Sân 11 người - Cỏ nhân tạo',     11),   -- id=3
    (2, 'Sân đơn - Trong nhà ',             2),   -- id=4
    (2, 'Sân đôi - Trong nhà',              4),   -- id=5
    (3, 'Sân 5vs5 - Ngoài trời',           10),   -- id=6
    (3, 'Sân 3vs3 - Trong nhà',             6),   -- id=7
    (4, 'Sân Pickleball trong nhà',          4),   -- id=8
    (4, 'Sân Pickleball ngoài trời',         4),   -- id=9
    (5, 'Sân bóng chuyền - Trong nhà',      10),   -- id=10
    (6, 'Sân đá cầu - Trong nhà',            4);   -- id=11

-- Sân — tất cả tại Sportify Hà Nội cơ sở 1 (location_id=1)
INSERT INTO fields (location_id, field_type_id, name, image_url, description, status) VALUES
    (1, 1,  'Sân bóng đá A1 (5 người)',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778992233/sportify/fields/sportify/fields/d3997d2b-eca9-4e98-91fb-7c9137f9c3cf.jpg',
     'Sân bóng đá 5 người với mặt cỏ nhân tạo chất lượng cao, không gian nhỏ gọn phù hợp cho các trận đấu giao hữu và luyện tập kỹ thuật. Sân được trang bị hệ thống đèn chiếu sáng hiện đại, lưới bao quanh và khu vực nghỉ ngơi tiện lợi.',
     'AVAILABLE'),   -- id=1

    (1, 2,  'Sân bóng đá B1 (7 người)',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778992409/sportify/fields/sportify/fields/29cc9030-8919-4b9d-8432-568536f3e666.jpg',
     'Sân bóng đá 7 người có diện tích rộng rãi hơn, phù hợp tổ chức các trận đấu phong trào và giải đấu bán chuyên. Mặt sân cỏ nhân tạo đạt chuẩn, hệ thống thoát nước tốt cùng ánh sáng đầy đủ giúp người chơi thi đấu thoải mái cả ngày lẫn đêm.',
     'AVAILABLE'),   -- id=2

    (1, 2,  'Sân bóng đá B2 (7 người)',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778992473/sportify/fields/sportify/fields/bb9ea265-f9cf-497b-aca5-0207888a84fb.jpg',
     'Sân bóng đá 7 người có diện tích rộng rãi hơn, phù hợp tổ chức các trận đấu phong trào và giải đấu bán chuyên. Mặt sân cỏ nhân tạo đạt chuẩn, hệ thống thoát nước tốt cùng ánh sáng đầy đủ giúp người chơi thi đấu thoải mái cả ngày lẫn đêm.',
     'AVAILABLE'),   -- id=3

    (1, 3,  'Sân bóng đá C1 (11 người)',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778992535/sportify/fields/sportify/fields/2ae1b8a5-5b32-4424-a744-5eddb46e979a.jpg',
     'Sân bóng đá 11 người có kích thước tiêu chuẩn rộng lớn, phù hợp tổ chức các trận đấu chuyên nghiệp, giải phong trào và hoạt động thể thao quy mô lớn. Mặt sân cỏ chất lượng cao, hệ thống đèn chiếu sáng hiện đại, khán đài và khu vực thay đồ đầy đủ mang lại trải nghiệm thi đấu tốt cho người chơi.',
     'AVAILABLE'),   -- id=4

    (1, 4,  'Sân cầu lông D1 (đơn)',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778992959/sportify/fields/sportify/fields/11dcf904-a995-42e0-b5a7-d8350bc2f2cb.webp',
     'Sân cầu lông trong nhà được trang bị mặt thảm PVC chất lượng cao, độ bám tốt và đảm bảo an toàn cho người chơi khi di chuyển. Không gian rộng rãi, hệ thống chiếu sáng hiện đại cùng trần cao đạt tiêu chuẩn giúp mang lại trải nghiệm thi đấu và luyện tập thoải mái.',
     'AVAILABLE'),   -- id=5

    (1, 5,  'Sân cầu lông D2 (đôi)',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778992988/sportify/fields/sportify/fields/ab4dbed8-5034-4135-92d5-f018f90d944e.webp',
     'Sân cầu lông trong nhà được trang bị mặt thảm PVC chất lượng cao, độ bám tốt và đảm bảo an toàn cho người chơi khi di chuyển. Không gian rộng rãi, hệ thống chiếu sáng hiện đại cùng trần cao đạt tiêu chuẩn giúp mang lại trải nghiệm thi đấu và luyện tập thoải mái.',
     'AVAILABLE'),   -- id=6

    (1, 7,  'Sân bóng rổ E1 3vs3',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778993362/sportify/fields/sportify/fields/abce4ff3-f21f-4408-88b7-00ad1030c08d.jpg',
     'Sân bóng rổ 3vs3 trong nhà được thiết kế theo phong cách hiện đại với mặt sân chống trơn trượt, hệ thống đèn chiếu sáng đạt chuẩn và không gian thoáng mát. Sân phù hợp cho các trận đấu giao hữu, luyện tập kỹ thuật và tổ chức giải đấu bóng rổ phong trào.',
     'AVAILABLE'),   -- id=7

    (1, 6,  'Sân bóng rổ E2 5vs5',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778993450/sportify/fields/sportify/fields/6e5024d7-ea9d-4462-9001-cae80e735420.jpg',
     'Sân bóng rổ 5vs5 ngoài trời có diện tích rộng rãi, mặt sân bền chắc cùng hệ thống rổ và vạch sân đạt tiêu chuẩn thi đấu. Không gian mở, thoáng đãng phù hợp cho hoạt động thể thao cộng đồng, luyện tập và tổ chức các trận đấu phong trào.',
     'AVAILABLE'),   -- id=8

    (1, 8,  'Sân Pickleball F1 trong nhà',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778995934/sportify/fields/sportify/fields/84826e30-82f3-4905-8a39-526927115b6f.jpg',
     'Sân Pickleball trong nhà được trang bị mặt sân chất lượng cao, chống trơn trượt cùng hệ thống đèn chiếu sáng hiện đại, mang lại không gian thi đấu thoải mái và ổn định. Sân phù hợp cho luyện tập, thi đấu giao hữu và tổ chức giải đấu bất kể điều kiện thời tiết.',
     'AVAILABLE'),   -- id=9

    (1, 9,  'Sân Pickleball F2 ngoài trời',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778995971/sportify/fields/sportify/fields/996b55dc-0d24-4fef-9789-edbf4edbd656.webp',
     'Sân Pickleball ngoài trời có không gian rộng rãi, thoáng mát với mặt sân bền chắc và hệ thống lưới đạt tiêu chuẩn. Đây là địa điểm lý tưởng cho các hoạt động thể thao, giải trí và giao lưu cộng đồng.',
     'AVAILABLE'),   -- id=10

    (1, 10, 'Sân bóng chuyền G1',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778996225/sportify/fields/sportify/fields/ee34b90e-92c8-4b49-b678-22c42aa03194.jpg',
     'Sân bóng chuyền được thiết kế rộng rãi với mặt sân bằng phẳng, hệ thống lưới chắc chắn và khu vực thi đấu đạt tiêu chuẩn. Không gian thoáng mát cùng hệ thống chiếu sáng hiện đại giúp người chơi luyện tập và thi đấu thoải mái cả ngày lẫn đêm.',
     'AVAILABLE'),   -- id=11

    (1, 11, 'Sân đá cầu H1 trong nhà',
     'https://res.cloudinary.com/drzkudoix/image/upload/v1778996264/sportify/fields/sportify/fields/fe777930-fb7d-4979-b3c8-64b16322ebbc.webp',
     'Sân đá cầu có diện tích phù hợp cho thi đấu và luyện tập phong trào, mặt sân sạch sẽ, an toàn và được trang bị lưới đạt chuẩn. Không gian thoáng đãng tạo điều kiện thuận lợi cho các hoạt động thể thao và giao lưu cộng đồng.',
     'AVAILABLE');   -- id=12

-- ── Bảng giá — Hà Nội cơ sở 1 (location_id=1) ───────────────────────────────
-- Khung giờ: 06:00-12:00 | 13:00-17:00 | 17:00-23:00 (12:00-13:00 là giờ nghỉ trưa)
-- Day type: WEEKDAY (Thứ 2-6) | WEEKEND (Thứ 7-CN)

-- Bóng đá 5 người (field_type_id=1)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 1, '06:00', '12:00', 100000.00, 'WEEKDAY'),
    (1, 1, '13:00', '17:00', 150000.00, 'WEEKDAY'),
    (1, 1, '17:00', '23:00', 200000.00, 'WEEKDAY'),
    (1, 1, '06:00', '12:00', 200000.00, 'WEEKEND'),
    (1, 1, '13:00', '17:00', 250000.00, 'WEEKEND'),
    (1, 1, '17:00', '23:00', 300000.00, 'WEEKEND');

-- Bóng đá 7 người (field_type_id=2)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 2, '06:00', '12:00', 100000.00, 'WEEKDAY'),
    (1, 2, '13:00', '17:00', 150000.00, 'WEEKDAY'),
    (1, 2, '17:00', '23:00', 200000.00, 'WEEKDAY'),
    (1, 2, '06:00', '12:00', 200000.00, 'WEEKEND'),
    (1, 2, '13:00', '17:00', 250000.00, 'WEEKEND'),
    (1, 2, '17:00', '23:00', 300000.00, 'WEEKEND');

-- Bóng đá 11 người (field_type_id=3)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 3, '06:00', '12:00', 100000.00, 'WEEKDAY'),
    (1, 3, '13:00', '17:00', 150000.00, 'WEEKDAY'),
    (1, 3, '17:00', '23:00', 200000.00, 'WEEKDAY'),
    (1, 3, '06:00', '12:00', 200000.00, 'WEEKEND'),
    (1, 3, '13:00', '17:00', 250000.00, 'WEEKEND'),
    (1, 3, '17:00', '23:00', 300000.00, 'WEEKEND');

-- Cầu lông đơn (field_type_id=4)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 4, '06:00', '12:00',  60000.00, 'WEEKDAY'),
    (1, 4, '13:00', '17:00',  80000.00, 'WEEKDAY'),
    (1, 4, '17:00', '23:00', 100000.00, 'WEEKDAY'),
    (1, 4, '06:00', '12:00',  90000.00, 'WEEKEND'),
    (1, 4, '13:00', '17:00', 110000.00, 'WEEKEND'),
    (1, 4, '17:00', '23:00', 130000.00, 'WEEKEND');

-- Cầu lông đôi (field_type_id=5)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 5, '06:00', '12:00',  70000.00, 'WEEKDAY'),
    (1, 5, '13:00', '17:00',  90000.00, 'WEEKDAY'),
    (1, 5, '17:00', '23:00', 120000.00, 'WEEKDAY'),
    (1, 5, '06:00', '12:00', 100000.00, 'WEEKEND'),
    (1, 5, '13:00', '17:00', 120000.00, 'WEEKEND'),
    (1, 5, '17:00', '23:00', 150000.00, 'WEEKEND');

-- Bóng rổ 5vs5 ngoài trời (field_type_id=6)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 6, '06:00', '12:00', 180000.00, 'WEEKDAY'),
    (1, 6, '13:00', '17:00', 220000.00, 'WEEKDAY'),
    (1, 6, '17:00', '23:00', 280000.00, 'WEEKDAY'),
    (1, 6, '06:00', '12:00', 250000.00, 'WEEKEND'),
    (1, 6, '13:00', '17:00', 300000.00, 'WEEKEND'),
    (1, 6, '17:00', '23:00', 360000.00, 'WEEKEND');

-- Bóng rổ 3vs3 trong nhà (field_type_id=7)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 7, '06:00', '12:00', 120000.00, 'WEEKDAY'),
    (1, 7, '13:00', '17:00', 160000.00, 'WEEKDAY'),
    (1, 7, '17:00', '23:00', 220000.00, 'WEEKDAY'),
    (1, 7, '06:00', '12:00', 180000.00, 'WEEKEND'),
    (1, 7, '13:00', '17:00', 220000.00, 'WEEKEND'),
    (1, 7, '17:00', '23:00', 280000.00, 'WEEKEND');

-- Pickleball trong nhà (field_type_id=8)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 8, '06:00', '12:00',  90000.00, 'WEEKDAY'),
    (1, 8, '13:00', '17:00', 120000.00, 'WEEKDAY'),
    (1, 8, '17:00', '23:00', 160000.00, 'WEEKDAY'),
    (1, 8, '06:00', '12:00', 140000.00, 'WEEKEND'),
    (1, 8, '13:00', '17:00', 170000.00, 'WEEKEND'),
    (1, 8, '17:00', '23:00', 210000.00, 'WEEKEND');

-- Pickleball ngoài trời (field_type_id=9)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 9, '06:00', '12:00',  70000.00, 'WEEKDAY'),
    (1, 9, '13:00', '17:00', 100000.00, 'WEEKDAY'),
    (1, 9, '17:00', '23:00', 140000.00, 'WEEKDAY'),
    (1, 9, '06:00', '12:00', 110000.00, 'WEEKEND'),
    (1, 9, '13:00', '17:00', 140000.00, 'WEEKEND'),
    (1, 9, '17:00', '23:00', 180000.00, 'WEEKEND');

-- Bóng chuyền trong nhà (field_type_id=10)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 10, '06:00', '12:00', 150000.00, 'WEEKDAY'),
    (1, 10, '13:00', '17:00', 200000.00, 'WEEKDAY'),
    (1, 10, '17:00', '23:00', 260000.00, 'WEEKDAY'),
    (1, 10, '06:00', '12:00', 220000.00, 'WEEKEND'),
    (1, 10, '13:00', '17:00', 270000.00, 'WEEKEND'),
    (1, 10, '17:00', '23:00', 330000.00, 'WEEKEND');

-- Đá cầu trong nhà (field_type_id=11)
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 11, '06:00', '12:00',  60000.00, 'WEEKDAY'),
    (1, 11, '13:00', '17:00',  80000.00, 'WEEKDAY'),
    (1, 11, '17:00', '23:00', 110000.00, 'WEEKDAY'),
    (1, 11, '06:00', '12:00',  90000.00, 'WEEKEND'),
    (1, 11, '13:00', '17:00', 110000.00, 'WEEKEND'),
    (1, 11, '17:00', '23:00', 140000.00, 'WEEKEND');

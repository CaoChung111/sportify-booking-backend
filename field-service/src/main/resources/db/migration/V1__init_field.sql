-- field-service: V1__init_field.sql
-- Schema + Seed data cho Sportify Field Service (Updated & Real Hanoi Data)

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

-- 1. Sports (8 môn thể thao)
INSERT INTO sports (id, name, slug) VALUES
(1, 'Bóng đá',     'bong-da'),
(2, 'Cầu lông',    'cau-long'),
(3, 'Bóng rổ',     'bong-ro'),
(4, 'Pickleball',  'pickleball'),
(5, 'Bóng chuyền', 'bong-chuyen'),
(6, 'Tennis',      'tennis'),
(7, 'Bóng bàn',    'bong-ban'),
(8, 'Đá cầu',      'da-cau');

-- 2. Locations (4 cơ sở thể thao thực tế tại Hà Nội)
INSERT INTO locations (id, name, address, region, hotline) VALUES
(1, 'Sportify Cầu Giấy',   'Số 15 Phố Duy Tân, Dịch Vọng Hậu, Cầu Giấy, Hà Nội',          'Hà Nội', '0901234001'),
(2, 'Sportify Thanh Xuân', 'Số 201 Nguyễn Trãi, Thanh Xuân Trung, Thanh Xuân, Hà Nội',   'Hà Nội', '0901234002'),
(3, 'Sportify Hà Đông',    'Số 88 Quang Trung, Hà Cầu, Hà Đông, Hà Nội',                 'Hà Nội', '0901234003'),
(4, 'Sportify Hoàng Mai',  'Số 52 Giải Phóng, Phương Liệt, Hoàng Mai, Hà Nội',            'Hà Nội', '0901234004');

-- 3. Field Types (23 loại sân thực tế)
INSERT INTO field_types (id, sport_id, name, player_capacity) VALUES
-- Bóng đá (sport_id=1)
(1,  1, 'Sân 5 người',                      10),
(2,  1, 'Sân 5 người chất lượng cao',       10),
(3,  1, 'Sân 7 người',                      14),
(4,  1, 'Sân 7 người chất lượng cao',       14),
-- Cầu lông (sport_id=2)
(5,  2, 'Sân tiêu chuẩn',                    4),
(6,  2, 'Sân chất lượng cao',                4),
(7,  2, 'Sân điều hòa',                      4),
-- Bóng rổ (sport_id=3)
(8,  3, 'Sân 3x3 ngoài trời',                6),
(9,  3, 'Sân 5x5 trong nhà',                10),
-- Pickleball (sport_id=4)
(10, 4, 'Sân ngoài trời',                    4),
(11, 4, 'Sân có mái che',                    4),
(12, 4, 'Sân trong nhà',                     4),
-- Bóng chuyền (sport_id=5)
(13, 5, 'Sân tiêu chuẩn',                   12),
(14, 5, 'Sân chất lượng cao',               12),
(15, 5, 'Sân bãi biển (cát)',                8),
-- Tennis (sport_id=6)
(16, 6, 'Sân ngoài trời',                    4),
(17, 6, 'Sân có mái che',                    4),
(18, 6, 'Sân trong nhà',                     4),
-- Bóng bàn (sport_id=7)
(19, 7, 'Bàn tiêu chuẩn',                    4),
(20, 7, 'Bàn chất lượng cao',                4),
(21, 7, 'Bàn VIP điều hòa',                  4),
-- Đá cầu (sport_id=8)
(22, 8, 'Sân tiêu chuẩn',                    4),
(23, 8, 'Sân chất lượng cao',                4);

-- 4. Fields (56 sân cụ thể phân bố hợp lý)
INSERT INTO fields (id, location_id, field_type_id, name, image_url, description, status) VALUES
-- ── Cơ sở 1: Cầu Giấy (16 sân: Bóng đá, Cầu lông, Pickleball, Bóng bàn, Đá cầu) ──
(1,  1, 1,  'Sân bóng đá 5A-CG',    'https://res.cloudinary.com/drzkudoix/image/upload/v1778992233/sportify/fields/sportify/fields/d3997d2b-eca9-4e98-91fb-7c9137f9c3cf.jpg', 'Sân bóng đá 5 người cỏ nhân tạo tiêu chuẩn, đèn chiếu sáng LED hiện đại.', 'AVAILABLE'),
(2,  1, 2,  'Sân bóng đá 5B-CG',    'https://res.cloudinary.com/drzkudoix/image/upload/v1778992233/sportify/fields/sportify/fields/d3997d2b-eca9-4e98-91fb-7c9137f9c3cf.jpg', 'Sân 5 người cỏ cao cấp hạt cao su êm ái, thích hợp cho trận đấu nhanh.', 'AVAILABLE'),
(3,  1, 3,  'Sân bóng đá 7A-CG',    'https://res.cloudinary.com/drzkudoix/image/upload/v1778992409/sportify/fields/sportify/fields/29cc9030-8919-4b9d-8432-568536f3e666.jpg', 'Sân bóng đá 7 người tiêu chuẩn phong trào, hệ thống lưới chắn bóng an toàn.', 'AVAILABLE'),
(4,  1, 3,  'Sân bóng đá 7B-CG',    'https://res.cloudinary.com/drzkudoix/image/upload/v1778992473/sportify/fields/sportify/fields/bb9ea265-f9cf-497b-aca5-0207888a84fb.jpg', 'Sân bóng đá 7 người khu B (đang bảo dưỡng cỏ).', 'MAINTENANCE'),
(5,  1, 5,  'Sân cầu lông 01-CG',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778992959/sportify/fields/sportify/fields/11dcf904-a995-42e0-b5a7-d8350bc2f2cb.webp', 'Sân cầu lông thảm PVC chống trượt, chiếu sáng chuẩn thi đấu.', 'AVAILABLE'),
(6,  1, 5,  'Sân cầu lông 02-CG',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778992959/sportify/fields/sportify/fields/11dcf904-a995-42e0-b5a7-d8350bc2f2cb.webp', 'Sân cầu lông thảm PVC thoáng mát.', 'AVAILABLE'),
(7,  1, 6,  'Sân cầu lông 03-CG',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778992988/sportify/fields/sportify/fields/ab4dbed8-5034-4135-92d5-f018f90d944e.webp', 'Sân cầu lông chất lượng cao với khoảng cách sân rộng rãi.', 'AVAILABLE'),
(8,  1, 7,  'Sân cầu lông 04-CG',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778992988/sportify/fields/sportify/fields/ab4dbed8-5034-4135-92d5-f018f90d944e.webp', 'Sân cầu lông phòng máy lạnh điều hòa nhiệt độ 24-26 độ C.', 'AVAILABLE'),
(9,  1, 10, 'Sân Pickleball 01-CG', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778995971/sportify/fields/sportify/fields/996b55dc-0d24-4fef-9789-edbf4edbd656.webp', 'Sân Pickleball ngoài trời thoáng đãng, bám bóng tốt.', 'AVAILABLE'),
(10, 1, 10, 'Sân Pickleball 02-CG', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778995971/sportify/fields/sportify/fields/996b55dc-0d24-4fef-9789-edbf4edbd656.webp', 'Sân Pickleball ngoài trời mặt sơn Silicon chuẩn Mỹ.', 'AVAILABLE'),
(11, 1, 11, 'Sân Pickleball 03-CG', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778995934/sportify/fields/sportify/fields/84826e30-82f3-4905-8a39-526927115b6f.jpg', 'Sân Pickleball có mái che chống mưa nắng.', 'AVAILABLE'),
(12, 1, 19, 'Bàn bóng bàn 01-CG',   NULL, 'Bàn bóng bàn Double Fish chuẩn thi đấu phong trào.', 'AVAILABLE'),
(13, 1, 19, 'Bàn bóng bàn 02-CG',   NULL, 'Bàn bóng bàn tiêu chuẩn thảm lót chân cao su.', 'AVAILABLE'),
(14, 1, 20, 'Bàn bóng bàn 03-CG',   NULL, 'Bàn bóng bàn chất lượng cao thương hiệu Butterfly.', 'AVAILABLE'),
(15, 1, 22, 'Sân đá cầu 01-CG',     'https://res.cloudinary.com/drzkudoix/image/upload/v1778996264/sportify/fields/sportify/fields/fe777930-fb7d-4979-b3c8-64b16322ebbc.webp', 'Sân đá cầu gỗ ván sàn mềm chống lật cổ chân.', 'AVAILABLE'),
(16, 1, 22, 'Sân đá cầu 02-CG',     'https://res.cloudinary.com/drzkudoix/image/upload/v1778996264/sportify/fields/sportify/fields/fe777930-fb7d-4979-b3c8-64b16322ebbc.webp', 'Sân đá cầu trong nhà lưới thi đấu chuẩn.', 'AVAILABLE'),

-- ── Cơ sở 2: Thanh Xuân (15 sân: Bóng đá, Cầu lông, Bóng rổ, Bóng chuyền, Bóng bàn) ──
(17, 2, 1,  'Sân bóng đá 5A-TX',    'https://res.cloudinary.com/drzkudoix/image/upload/v1778992233/sportify/fields/sportify/fields/d3997d2b-eca9-4e98-91fb-7c9137f9c3cf.jpg', 'Sân bóng đá 5 người cỏ nhân tạo đạt chuẩn.', 'AVAILABLE'),
(18, 2, 1,  'Sân bóng đá 5B-TX',    'https://res.cloudinary.com/drzkudoix/image/upload/v1778992233/sportify/fields/sportify/fields/d3997d2b-eca9-4e98-91fb-7c9137f9c3cf.jpg', 'Sân bóng đá 5 người góc quan sát tốt.', 'AVAILABLE'),
(19, 2, 3,  'Sân bóng đá 7A-TX',    'https://res.cloudinary.com/drzkudoix/image/upload/v1778992409/sportify/fields/sportify/fields/29cc9030-8919-4b9d-8432-568536f3e666.jpg', 'Sân 7 người rộng rãi, thoát nước mặt sân cực tốt.', 'AVAILABLE'),
(20, 2, 4,  'Sân bóng đá 7B-TX',    'https://res.cloudinary.com/drzkudoix/image/upload/v1778992473/sportify/fields/sportify/fields/bb9ea265-f9cf-497b-aca5-0207888a84fb.jpg', 'Sân 7 người cỏ sợi kim kim cương chống lật cổ chân.', 'AVAILABLE'),
(21, 2, 5,  'Sân cầu lông 01-TX',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778992959/sportify/fields/sportify/fields/11dcf904-a995-42e0-b5a7-d8350bc2f2cb.webp', 'Sân cầu lông thảm PVC xanh ngọc bích.', 'AVAILABLE'),
(22, 2, 6,  'Sân cầu lông 02-TX',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778992988/sportify/fields/sportify/fields/ab4dbed8-5034-4135-92d5-f018f90d944e.webp', 'Sân cầu lông cao cấp có khán đài mini.', 'AVAILABLE'),
(23, 2, 8,  'Sân bóng rổ 3x3-TX',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778993362/sportify/fields/sportify/fields/abce4ff3-f21f-4408-88b7-00ad1030c08d.jpg', 'Sân rổ 3x3 ngoài trời mặt sơn Acrylic.', 'AVAILABLE'),
(24, 2, 9,  'Sân bóng rổ 5x5 A-TX', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778993450/sportify/fields/sportify/fields/6e5024d7-ea9d-4462-9001-cae80e735420.jpg', 'Sân rổ 5x5 trong nhà sàn gỗ Maple tiêu chuẩn NBA.', 'AVAILABLE'),
(25, 2, 9,  'Sân bóng rổ 5x5 B-TX', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778993450/sportify/fields/sportify/fields/6e5024d7-ea9d-4462-9001-cae80e735420.jpg', 'Sân rổ 5x5 khu B (đang làm lại mặt sàn).', 'MAINTENANCE'),
(26, 2, 13, 'Sân bóng chuyền 01-TX', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778996225/sportify/fields/sportify/fields/ee34b90e-92c8-4b49-b678-22c42aa03194.jpg', 'Sân bóng chuyền thảm cao su chuyên dụng.', 'AVAILABLE'),
(27, 2, 14, 'Sân bóng chuyền 02-TX', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778996225/sportify/fields/sportify/fields/ee34b90e-92c8-4b49-b678-22c42aa03194.jpg', 'Sân bóng chuyền chất lượng cao có lưới căng tự động.', 'AVAILABLE'),
(28, 2, 19, 'Bàn bóng bàn 01-TX',   NULL, 'Bàn bóng bàn chuẩn thi đấu phong trào.', 'AVAILABLE'),
(29, 2, 19, 'Bàn bóng bàn 02-TX',   NULL, 'Bàn bóng bàn tiêu chuẩn sạch sẽ.', 'AVAILABLE'),
(30, 2, 20, 'Bàn bóng bàn 03-TX',   NULL, 'Bàn bóng bàn chống lóa 25mm.', 'AVAILABLE'),
(31, 2, 21, 'Bàn bóng bàn VIP-TX',  NULL, 'Phòng bóng bàn VIP điều hòa riêng biệt, TV xài nước giải khát.', 'AVAILABLE'),

-- ── Cơ sở 3: Hà Đông (12 sân: Bóng đá, Tennis, Pickleball, Đá cầu, Bóng chuyền bãi biển) ──
(32, 3, 1,  'Sân bóng đá 5A-HĐ',    'https://res.cloudinary.com/drzkudoix/image/upload/v1778992233/sportify/fields/sportify/fields/d3997d2b-eca9-4e98-91fb-7c9137f9c3cf.jpg', 'Sân 5 người thoáng mát khu vực Hà Đông.', 'AVAILABLE'),
(33, 3, 2,  'Sân bóng đá 5B-HĐ',    'https://res.cloudinary.com/drzkudoix/image/upload/v1778992233/sportify/fields/sportify/fields/d3997d2b-eca9-4e98-91fb-7c9137f9c3cf.jpg', 'Sân 5 người cỏ nhân tạo mềm mại.', 'AVAILABLE'),
(34, 3, 16, 'Sân Tennis 01-HĐ',     NULL, 'Sân Tennis ngoài trời mặt đệm giảm xóc Flexipave.', 'AVAILABLE'),
(35, 3, 17, 'Sân Tennis 02-HĐ',     NULL, 'Sân Tennis mái che không lo mưa nắng.', 'AVAILABLE'),
(36, 3, 18, 'Sân Tennis 03-HĐ',     NULL, 'Sân Tennis trong nhà máy lạnh tiêu chuẩn Grand Slam.', 'AVAILABLE'),
(37, 3, 10, 'Sân Pickleball 01-HĐ', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778995971/sportify/fields/sportify/fields/996b55dc-0d24-4fef-9789-edbf4edbd656.webp', 'Sân Pickleball ngoài trời khu vực Hà Đông.', 'AVAILABLE'),
(38, 3, 10, 'Sân Pickleball 02-HĐ', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778995971/sportify/fields/sportify/fields/996b55dc-0d24-4fef-9789-edbf4edbd656.webp', 'Sân Pickleball ngoài trời rộng rãi.', 'AVAILABLE'),
(39, 3, 12, 'Sân Pickleball 03-HĐ', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778995934/sportify/fields/sportify/fields/84826e30-82f3-4905-8a39-526927115b6f.jpg', 'Sân Pickleball trong nhà thảm Silicon mềm.', 'AVAILABLE'),
(40, 3, 22, 'Sân đá cầu 01-HĐ',     'https://res.cloudinary.com/drzkudoix/image/upload/v1778996264/sportify/fields/sportify/fields/fe777930-fb7d-4979-b3c8-64b16322ebbc.webp', 'Sân đá cầu sàn gỗ thảm tổng hợp.', 'AVAILABLE'),
(41, 3, 23, 'Sân đá cầu 02-HĐ',     'https://res.cloudinary.com/drzkudoix/image/upload/v1778996264/sportify/fields/sportify/fields/fe777930-fb7d-4979-b3c8-64b16322ebbc.webp', 'Sân đá cầu chất lượng cao.', 'AVAILABLE'),
(42, 3, 15, 'Sân BC Bãi Biển-HĐ',  'https://res.cloudinary.com/drzkudoix/image/upload/v1778996225/sportify/fields/sportify/fields/ee34b90e-92c8-4b49-b678-22c42aa03194.jpg', 'Sân bóng chuyền bãi biển cát trắng mịn độc đáo duy nhất Hà Đông.', 'AVAILABLE'),
(43, 3, 13, 'Sân bóng chuyền 01-HĐ', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778996225/sportify/fields/sportify/fields/ee34b90e-92c8-4b49-b678-22c42aa03194.jpg', 'Sân bóng chuyền tiêu chuẩn ngoài trời.', 'AVAILABLE'),

-- ── Cơ sở 4: Hoàng Mai (13 sân: Cầu lông, Bóng rổ, Tennis, Pickleball, Bóng chuyền, Đá cầu) ──
(44, 4, 5,  'Sân cầu lông 01-HM',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778992959/sportify/fields/sportify/fields/11dcf904-a995-42e0-b5a7-d8350bc2f2cb.webp', 'Sân cầu lông thảm PVC Hoàng Mai.', 'AVAILABLE'),
(45, 4, 5,  'Sân cầu lông 02-HM',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778992959/sportify/fields/sportify/fields/11dcf904-a995-42e0-b5a7-d8350bc2f2cb.webp', 'Sân cầu lông chiếu sáng tốt.', 'AVAILABLE'),
(46, 4, 7,  'Sân cầu lông 03-HM',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778992988/sportify/fields/sportify/fields/ab4dbed8-5034-4135-92d5-f018f90d944e.webp', 'Sân cầu lông điều hòa mát mẻ.', 'AVAILABLE'),
(47, 4, 8,  'Sân bóng rổ 3x3-HM',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778993362/sportify/fields/sportify/fields/abce4ff3-f21f-4408-88b7-00ad1030c08d.jpg', 'Sân bóng rổ 3x3 sơn acrylic chống trơn.', 'AVAILABLE'),
(48, 4, 9,  'Sân bóng rổ 5x5-HM',   'https://res.cloudinary.com/drzkudoix/image/upload/v1778993450/sportify/fields/sportify/fields/6e5024d7-ea9d-4462-9001-cae80e735420.jpg', 'Sân rổ 5x5 tiêu chuẩn thi đấu.', 'AVAILABLE'),
(49, 4, 11, 'Sân Pickleball 01-HM', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778995971/sportify/fields/sportify/fields/996b55dc-0d24-4fef-9789-edbf4edbd656.webp', 'Sân Pickleball mái che Hoàng Mai.', 'AVAILABLE'),
(50, 4, 12, 'Sân Pickleball 02-HM', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778995934/sportify/fields/sportify/fields/84826e30-82f3-4905-8a39-526927115b6f.jpg', 'Sân Pickleball trong nhà thảm êm.', 'AVAILABLE'),
(51, 4, 16, 'Sân Tennis 01-HM',     NULL, 'Sân Tennis ngoài trời đệm chống trượt.', 'AVAILABLE'),
(52, 4, 17, 'Sân Tennis 02-HM',     NULL, 'Sân Tennis mái che chống nắng mưa.', 'AVAILABLE'),
(53, 4, 13, 'Sân bóng chuyền 01-HM', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778996225/sportify/fields/sportify/fields/ee34b90e-92c8-4b49-b678-22c42aa03194.jpg', 'Sân bóng chuyền tiêu chuẩn thảm PVC.', 'AVAILABLE'),
(54, 4, 14, 'Sân bóng chuyền 02-HM', 'https://res.cloudinary.com/drzkudoix/image/upload/v1778996225/sportify/fields/sportify/fields/ee34b90e-92c8-4b49-b678-22c42aa03194.jpg', 'Sân bóng chuyền chất lượng cao sàn gỗ.', 'AVAILABLE'),
(55, 4, 22, 'Sân đá cầu 01-HM',     'https://res.cloudinary.com/drzkudoix/image/upload/v1778996264/sportify/fields/sportify/fields/fe777930-fb7d-4979-b3c8-64b16322ebbc.webp', 'Sân đá cầu tiêu chuẩn sạch sẽ.', 'AVAILABLE'),
(56, 4, 23, 'Sân đá cầu 02-HM',     'https://res.cloudinary.com/drzkudoix/image/upload/v1778996264/sportify/fields/sportify/fields/fe777930-fb7d-4979-b3c8-64b16322ebbc.webp', 'Sân đá cầu chất lượng cao ánh sáng đều.', 'AVAILABLE');

-- 5. Prices (Bảng giá hợp lý thực tế thị trường Hà Nội)
-- Khung giờ: 06:00-12:00 (sáng), 13:00-17:00 (chiều), 17:00-23:00 (tối cao điểm)
-- DayType: WEEKDAY, WEEKEND

INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES

-- ══════════════════════════════════════════════════════════════════════════════
-- CƠ SỞ 1: SPORTIFY CẦU GIẤY (location_id=1)
-- ══════════════════════════════════════════════════════════════════════════════
-- ft=1: Bóng đá 5 người
(1, 1, '06:00', '12:00', 200000.00, 'WEEKDAY'), (1, 1, '13:00', '17:00', 250000.00, 'WEEKDAY'), (1, 1, '17:00', '23:00', 300000.00, 'WEEKDAY'),
(1, 1, '06:00', '12:00', 280000.00, 'WEEKEND'), (1, 1, '13:00', '17:00', 320000.00, 'WEEKEND'), (1, 1, '17:00', '23:00', 380000.00, 'WEEKEND'),
-- ft=2: Bóng đá 5 CLC
(1, 2, '06:00', '12:00', 280000.00, 'WEEKDAY'), (1, 2, '13:00', '17:00', 320000.00, 'WEEKDAY'), (1, 2, '17:00', '23:00', 380000.00, 'WEEKDAY'),
(1, 2, '06:00', '12:00', 350000.00, 'WEEKEND'), (1, 2, '13:00', '17:00', 400000.00, 'WEEKEND'), (1, 2, '17:00', '23:00', 450000.00, 'WEEKEND'),
-- ft=3: Bóng đá 7 người
(1, 3, '06:00', '12:00', 350000.00, 'WEEKDAY'), (1, 3, '13:00', '17:00', 420000.00, 'WEEKDAY'), (1, 3, '17:00', '23:00', 500000.00, 'WEEKDAY'),
(1, 3, '06:00', '12:00', 450000.00, 'WEEKEND'), (1, 3, '13:00', '17:00', 520000.00, 'WEEKEND'), (1, 3, '17:00', '23:00', 600000.00, 'WEEKEND'),
-- ft=5: Cầu lông tiêu chuẩn
(1, 5, '06:00', '12:00',  50000.00, 'WEEKDAY'), (1, 5, '13:00', '17:00',  65000.00, 'WEEKDAY'), (1, 5, '17:00', '23:00',  80000.00, 'WEEKDAY'),
(1, 5, '06:00', '12:00',  70000.00, 'WEEKEND'), (1, 5, '13:00', '17:00',  85000.00, 'WEEKEND'), (1, 5, '17:00', '23:00', 100000.00, 'WEEKEND'),
-- ft=6: Cầu lông CLC
(1, 6, '06:00', '12:00',  70000.00, 'WEEKDAY'), (1, 6, '13:00', '17:00',  85000.00, 'WEEKDAY'), (1, 6, '17:00', '23:00', 100000.00, 'WEEKDAY'),
(1, 6, '06:00', '12:00',  90000.00, 'WEEKEND'), (1, 6, '13:00', '17:00', 105000.00, 'WEEKEND'), (1, 6, '17:00', '23:00', 120000.00, 'WEEKEND'),
-- ft=7: Cầu lông điều hòa
(1, 7, '06:00', '12:00',  90000.00, 'WEEKDAY'), (1, 7, '13:00', '17:00', 110000.00, 'WEEKDAY'), (1, 7, '17:00', '23:00', 130000.00, 'WEEKDAY'),
(1, 7, '06:00', '12:00', 110000.00, 'WEEKEND'), (1, 7, '13:00', '17:00', 130000.00, 'WEEKEND'), (1, 7, '17:00', '23:00', 150000.00, 'WEEKEND'),
-- ft=10: Pickleball ngoài trời
(1, 10, '06:00', '12:00', 100000.00, 'WEEKDAY'), (1, 10, '13:00', '17:00', 120000.00, 'WEEKDAY'), (1, 10, '17:00', '23:00', 150000.00, 'WEEKDAY'),
(1, 10, '06:00', '12:00', 130000.00, 'WEEKEND'), (1, 10, '13:00', '17:00', 150000.00, 'WEEKEND'), (1, 10, '17:00', '23:00', 180000.00, 'WEEKEND'),
-- ft=11: Pickleball mái che
(1, 11, '06:00', '12:00', 120000.00, 'WEEKDAY'), (1, 11, '13:00', '17:00', 140000.00, 'WEEKDAY'), (1, 11, '17:00', '23:00', 170000.00, 'WEEKDAY'),
(1, 11, '06:00', '12:00', 150000.00, 'WEEKEND'), (1, 11, '13:00', '17:00', 170000.00, 'WEEKEND'), (1, 11, '17:00', '23:00', 200000.00, 'WEEKEND'),
-- ft=19: Bóng bàn tiêu chuẩn
(1, 19, '06:00', '12:00',  30000.00, 'WEEKDAY'), (1, 19, '13:00', '17:00',  40000.00, 'WEEKDAY'), (1, 19, '17:00', '23:00',  50000.00, 'WEEKDAY'),
(1, 19, '06:00', '12:00',  40000.00, 'WEEKEND'), (1, 19, '13:00', '17:00',  50000.00, 'WEEKEND'), (1, 19, '17:00', '23:00',  60000.00, 'WEEKEND'),
-- ft=20: Bóng bàn CLC
(1, 20, '06:00', '12:00',  40000.00, 'WEEKDAY'), (1, 20, '13:00', '17:00',  55000.00, 'WEEKDAY'), (1, 20, '17:00', '23:00',  70000.00, 'WEEKDAY'),
(1, 20, '06:00', '12:00',  55000.00, 'WEEKEND'), (1, 20, '13:00', '17:00',  70000.00, 'WEEKEND'), (1, 20, '17:00', '23:00',  85000.00, 'WEEKEND'),
-- ft=22: Đá cầu tiêu chuẩn
(1, 22, '06:00', '12:00',  40000.00, 'WEEKDAY'), (1, 22, '13:00', '17:00',  55000.00, 'WEEKDAY'), (1, 22, '17:00', '23:00',  70000.00, 'WEEKDAY'),
(1, 22, '06:00', '12:00',  55000.00, 'WEEKEND'), (1, 22, '13:00', '17:00',  70000.00, 'WEEKEND'), (1, 22, '17:00', '23:00',  85000.00, 'WEEKEND'),

-- ══════════════════════════════════════════════════════════════════════════════
-- CƠ SỞ 2: SPORTIFY THANH XUÂN (location_id=2)
-- ══════════════════════════════════════════════════════════════════════════════
-- ft=1: Bóng đá 5 người
(2, 1, '06:00', '12:00', 200000.00, 'WEEKDAY'), (2, 1, '13:00', '17:00', 250000.00, 'WEEKDAY'), (2, 1, '17:00', '23:00', 300000.00, 'WEEKDAY'),
(2, 1, '06:00', '12:00', 280000.00, 'WEEKEND'), (2, 1, '13:00', '17:00', 320000.00, 'WEEKEND'), (2, 1, '17:00', '23:00', 380000.00, 'WEEKEND'),
-- ft=3: Bóng đá 7 người
(2, 3, '06:00', '12:00', 350000.00, 'WEEKDAY'), (2, 3, '13:00', '17:00', 420000.00, 'WEEKDAY'), (2, 3, '17:00', '23:00', 500000.00, 'WEEKDAY'),
(2, 3, '06:00', '12:00', 450000.00, 'WEEKEND'), (2, 3, '13:00', '17:00', 520000.00, 'WEEKEND'), (2, 3, '17:00', '23:00', 600000.00, 'WEEKEND'),
-- ft=4: Bóng đá 7 CLC
(2, 4, '06:00', '12:00', 420000.00, 'WEEKDAY'), (2, 4, '13:00', '17:00', 500000.00, 'WEEKDAY'), (2, 4, '17:00', '23:00', 580000.00, 'WEEKDAY'),
(2, 4, '06:00', '12:00', 520000.00, 'WEEKEND'), (2, 4, '13:00', '17:00', 600000.00, 'WEEKEND'), (2, 4, '17:00', '23:00', 680000.00, 'WEEKEND'),
-- ft=5: Cầu lông tiêu chuẩn
(2, 5, '06:00', '12:00',  50000.00, 'WEEKDAY'), (2, 5, '13:00', '17:00',  65000.00, 'WEEKDAY'), (2, 5, '17:00', '23:00',  80000.00, 'WEEKDAY'),
(2, 5, '06:00', '12:00',  70000.00, 'WEEKEND'), (2, 5, '13:00', '17:00',  85000.00, 'WEEKEND'), (2, 5, '17:00', '23:00', 100000.00, 'WEEKEND'),
-- ft=6: Cầu lông CLC
(2, 6, '06:00', '12:00',  70000.00, 'WEEKDAY'), (2, 6, '13:00', '17:00',  85000.00, 'WEEKDAY'), (2, 6, '17:00', '23:00', 100000.00, 'WEEKDAY'),
(2, 6, '06:00', '12:00',  90000.00, 'WEEKEND'), (2, 6, '13:00', '17:00', 105000.00, 'WEEKEND'), (2, 6, '17:00', '23:00', 120000.00, 'WEEKEND'),
-- ft=8: Bóng rổ 3x3
(2, 8, '06:00', '12:00', 100000.00, 'WEEKDAY'), (2, 8, '13:00', '17:00', 130000.00, 'WEEKDAY'), (2, 8, '17:00', '23:00', 160000.00, 'WEEKDAY'),
(2, 8, '06:00', '12:00', 140000.00, 'WEEKEND'), (2, 8, '13:00', '17:00', 170000.00, 'WEEKEND'), (2, 8, '17:00', '23:00', 200000.00, 'WEEKEND'),
-- ft=9: Bóng rổ 5x5
(2, 9, '06:00', '12:00', 160000.00, 'WEEKDAY'), (2, 9, '13:00', '17:00', 200000.00, 'WEEKDAY'), (2, 9, '17:00', '23:00', 250000.00, 'WEEKDAY'),
(2, 9, '06:00', '12:00', 220000.00, 'WEEKEND'), (2, 9, '13:00', '17:00', 260000.00, 'WEEKEND'), (2, 9, '17:00', '23:00', 310000.00, 'WEEKEND'),
-- ft=13: Bóng chuyền tiêu chuẩn
(2, 13, '06:00', '12:00', 150000.00, 'WEEKDAY'), (2, 13, '13:00', '17:00', 180000.00, 'WEEKDAY'), (2, 13, '17:00', '23:00', 220000.00, 'WEEKDAY'),
(2, 13, '06:00', '12:00', 200000.00, 'WEEKEND'), (2, 13, '13:00', '17:00', 240000.00, 'WEEKEND'), (2, 13, '17:00', '23:00', 290000.00, 'WEEKEND'),
-- ft=14: Bóng chuyền CLC
(2, 14, '06:00', '12:00', 200000.00, 'WEEKDAY'), (2, 14, '13:00', '17:00', 240000.00, 'WEEKDAY'), (2, 14, '17:00', '23:00', 280000.00, 'WEEKDAY'),
(2, 14, '06:00', '12:00', 260000.00, 'WEEKEND'), (2, 14, '13:00', '17:00', 300000.00, 'WEEKEND'), (2, 14, '17:00', '23:00', 350000.00, 'WEEKEND'),
-- ft=19: Bóng bàn tiêu chuẩn
(2, 19, '06:00', '12:00',  30000.00, 'WEEKDAY'), (2, 19, '13:00', '17:00',  40000.00, 'WEEKDAY'), (2, 19, '17:00', '23:00',  50000.00, 'WEEKDAY'),
(2, 19, '06:00', '12:00',  40000.00, 'WEEKEND'), (2, 19, '13:00', '17:00',  50000.00, 'WEEKEND'), (2, 19, '17:00', '23:00',  60000.00, 'WEEKEND'),
-- ft=20: Bóng bàn CLC
(2, 20, '06:00', '12:00',  40000.00, 'WEEKDAY'), (2, 20, '13:00', '17:00',  55000.00, 'WEEKDAY'), (2, 20, '17:00', '23:00',  70000.00, 'WEEKDAY'),
(2, 20, '06:00', '12:00',  55000.00, 'WEEKEND'), (2, 20, '13:00', '17:00',  70000.00, 'WEEKEND'), (2, 20, '17:00', '23:00',  85000.00, 'WEEKEND'),
-- ft=21: Bóng bàn VIP điều hòa
(2, 21, '06:00', '12:00',  60000.00, 'WEEKDAY'), (2, 21, '13:00', '17:00',  75000.00, 'WEEKDAY'), (2, 21, '17:00', '23:00',  90000.00, 'WEEKDAY'),
(2, 21, '06:00', '12:00',  75000.00, 'WEEKEND'), (2, 21, '13:00', '17:00',  90000.00, 'WEEKEND'), (2, 21, '17:00', '23:00', 110000.00, 'WEEKEND'),

-- ══════════════════════════════════════════════════════════════════════════════
-- CƠ SỞ 3: SPORTIFY HÀ ĐÔNG (location_id=3) — Giá ưu đãi hơn 10%
-- ══════════════════════════════════════════════════════════════════════════════
-- ft=1: Bóng đá 5 người
(3, 1, '06:00', '12:00', 180000.00, 'WEEKDAY'), (3, 1, '13:00', '17:00', 220000.00, 'WEEKDAY'), (3, 1, '17:00', '23:00', 270000.00, 'WEEKDAY'),
(3, 1, '06:00', '12:00', 250000.00, 'WEEKEND'), (3, 1, '13:00', '17:00', 290000.00, 'WEEKEND'), (3, 1, '17:00', '23:00', 340000.00, 'WEEKEND'),
-- ft=2: Bóng đá 5 CLC
(3, 2, '06:00', '12:00', 250000.00, 'WEEKDAY'), (3, 2, '13:00', '17:00', 290000.00, 'WEEKDAY'), (3, 2, '17:00', '23:00', 340000.00, 'WEEKDAY'),
(3, 2, '06:00', '12:00', 310000.00, 'WEEKEND'), (3, 2, '13:00', '17:00', 360000.00, 'WEEKEND'), (3, 2, '17:00', '23:00', 400000.00, 'WEEKEND'),
-- ft=10: Pickleball ngoài trời
(3, 10, '06:00', '12:00',  90000.00, 'WEEKDAY'), (3, 10, '13:00', '17:00', 110000.00, 'WEEKDAY'), (3, 10, '17:00', '23:00', 135000.00, 'WEEKDAY'),
(3, 10, '06:00', '12:00', 115000.00, 'WEEKEND'), (3, 10, '13:00', '17:00', 135000.00, 'WEEKEND'), (3, 10, '17:00', '23:00', 160000.00, 'WEEKEND'),
-- ft=12: Pickleball trong nhà
(3, 12, '06:00', '12:00', 135000.00, 'WEEKDAY'), (3, 12, '13:00', '17:00', 160000.00, 'WEEKDAY'), (3, 12, '17:00', '23:00', 200000.00, 'WEEKDAY'),
(3, 12, '06:00', '12:00', 160000.00, 'WEEKEND'), (3, 12, '13:00', '17:00', 190000.00, 'WEEKEND'), (3, 12, '17:00', '23:00', 230000.00, 'WEEKEND'),
-- ft=13: Bóng chuyền tiêu chuẩn
(3, 13, '06:00', '12:00', 135000.00, 'WEEKDAY'), (3, 13, '13:00', '17:00', 160000.00, 'WEEKDAY'), (3, 13, '17:00', '23:00', 200000.00, 'WEEKDAY'),
(3, 13, '06:00', '12:00', 180000.00, 'WEEKEND'), (3, 13, '13:00', '17:00', 215000.00, 'WEEKEND'), (3, 13, '17:00', '23:00', 260000.00, 'WEEKEND'),
-- ft=15: Bóng chuyền bãi biển
(3, 15, '06:00', '12:00', 160000.00, 'WEEKDAY'), (3, 15, '13:00', '17:00', 200000.00, 'WEEKDAY'), (3, 15, '17:00', '23:00', 235000.00, 'WEEKDAY'),
(3, 15, '06:00', '12:00', 215000.00, 'WEEKEND'), (3, 15, '13:00', '17:00', 250000.00, 'WEEKEND'), (3, 15, '17:00', '23:00', 290000.00, 'WEEKEND'),
-- ft=16: Tennis ngoài trời
(3, 16, '06:00', '12:00', 110000.00, 'WEEKDAY'), (3, 16, '13:00', '17:00', 135000.00, 'WEEKDAY'), (3, 16, '17:00', '23:00', 160000.00, 'WEEKDAY'),
(3, 16, '06:00', '12:00', 145000.00, 'WEEKEND'), (3, 16, '13:00', '17:00', 170000.00, 'WEEKEND'), (3, 16, '17:00', '23:00', 200000.00, 'WEEKEND'),
-- ft=17: Tennis mái che
(3, 17, '06:00', '12:00', 145000.00, 'WEEKDAY'), (3, 17, '13:00', '17:00', 170000.00, 'WEEKDAY'), (3, 17, '17:00', '23:00', 205000.00, 'WEEKDAY'),
(3, 17, '06:00', '12:00', 180000.00, 'WEEKEND'), (3, 17, '13:00', '17:00', 215000.00, 'WEEKEND'), (3, 17, '17:00', '23:00', 250000.00, 'WEEKEND'),
-- ft=18: Tennis trong nhà
(3, 18, '06:00', '12:00', 200000.00, 'WEEKDAY'), (3, 18, '13:00', '17:00', 235000.00, 'WEEKDAY'), (3, 18, '17:00', '23:00', 280000.00, 'WEEKDAY'),
(3, 18, '06:00', '12:00', 245000.00, 'WEEKEND'), (3, 18, '13:00', '17:00', 280000.00, 'WEEKEND'), (3, 18, '17:00', '23:00', 325000.00, 'WEEKEND'),
-- ft=22: Đá cầu tiêu chuẩn
(3, 22, '06:00', '12:00',  35000.00, 'WEEKDAY'), (3, 22, '13:00', '17:00',  50000.00, 'WEEKDAY'), (3, 22, '17:00', '23:00',  65000.00, 'WEEKDAY'),
(3, 22, '06:00', '12:00',  50000.00, 'WEEKEND'), (3, 22, '13:00', '17:00',  65000.00, 'WEEKEND'), (3, 22, '17:00', '23:00',  75000.00, 'WEEKEND'),
-- ft=23: Đá cầu CLC
(3, 23, '06:00', '12:00',  50000.00, 'WEEKDAY'), (3, 23, '13:00', '17:00',  65000.00, 'WEEKDAY'), (3, 23, '17:00', '23:00',  80000.00, 'WEEKDAY'),
(3, 23, '06:00', '12:00',  65000.00, 'WEEKEND'), (3, 23, '13:00', '17:00',  80000.00, 'WEEKEND'), (3, 23, '17:00', '23:00', 100000.00, 'WEEKEND'),

-- ══════════════════════════════════════════════════════════════════════════════
-- CƠ SỞ 4: SPORTIFY HOÀNG MAI (location_id=4)
-- ══════════════════════════════════════════════════════════════════════════════
-- ft=5: Cầu lông tiêu chuẩn
(4, 5, '06:00', '12:00',  50000.00, 'WEEKDAY'), (4, 5, '13:00', '17:00',  65000.00, 'WEEKDAY'), (4, 5, '17:00', '23:00',  80000.00, 'WEEKDAY'),
(4, 5, '06:00', '12:00',  70000.00, 'WEEKEND'), (4, 5, '13:00', '17:00',  85000.00, 'WEEKEND'), (4, 5, '17:00', '23:00', 100000.00, 'WEEKEND'),
-- ft=7: Cầu lông điều hòa
(4, 7, '06:00', '12:00',  90000.00, 'WEEKDAY'), (4, 7, '13:00', '17:00', 110000.00, 'WEEKDAY'), (4, 7, '17:00', '23:00', 130000.00, 'WEEKDAY'),
(4, 7, '06:00', '12:00', 110000.00, 'WEEKEND'), (4, 7, '13:00', '17:00', 130000.00, 'WEEKEND'), (4, 7, '17:00', '23:00', 150000.00, 'WEEKEND'),
-- ft=8: Bóng rổ 3x3
(4, 8, '06:00', '12:00', 100000.00, 'WEEKDAY'), (4, 8, '13:00', '17:00', 130000.00, 'WEEKDAY'), (4, 8, '17:00', '23:00', 160000.00, 'WEEKDAY'),
(4, 8, '06:00', '12:00', 140000.00, 'WEEKEND'), (4, 8, '13:00', '17:00', 170000.00, 'WEEKEND'), (4, 8, '17:00', '23:00', 200000.00, 'WEEKEND'),
-- ft=9: Bóng rổ 5x5
(4, 9, '06:00', '12:00', 160000.00, 'WEEKDAY'), (4, 9, '13:00', '17:00', 200000.00, 'WEEKDAY'), (4, 9, '17:00', '23:00', 250000.00, 'WEEKDAY'),
(4, 9, '06:00', '12:00', 220000.00, 'WEEKEND'), (4, 9, '13:00', '17:00', 260000.00, 'WEEKEND'), (4, 9, '17:00', '23:00', 310000.00, 'WEEKEND'),
-- ft=11: Pickleball mái che
(4, 11, '06:00', '12:00', 120000.00, 'WEEKDAY'), (4, 11, '13:00', '17:00', 140000.00, 'WEEKDAY'), (4, 11, '17:00', '23:00', 170000.00, 'WEEKDAY'),
(4, 11, '06:00', '12:00', 150000.00, 'WEEKEND'), (4, 11, '13:00', '17:00', 170000.00, 'WEEKEND'), (4, 11, '17:00', '23:00', 200000.00, 'WEEKEND'),
-- ft=12: Pickleball trong nhà
(4, 12, '06:00', '12:00', 150000.00, 'WEEKDAY'), (4, 12, '13:00', '17:00', 180000.00, 'WEEKDAY'), (4, 12, '17:00', '23:00', 220000.00, 'WEEKDAY'),
(4, 12, '06:00', '12:00', 180000.00, 'WEEKEND'), (4, 12, '13:00', '17:00', 210000.00, 'WEEKEND'), (4, 12, '17:00', '23:00', 260000.00, 'WEEKEND'),
-- ft=13: Bóng chuyền tiêu chuẩn
(4, 13, '06:00', '12:00', 150000.00, 'WEEKDAY'), (4, 13, '13:00', '17:00', 180000.00, 'WEEKDAY'), (4, 13, '17:00', '23:00', 220000.00, 'WEEKDAY'),
(4, 13, '06:00', '12:00', 200000.00, 'WEEKEND'), (4, 13, '13:00', '17:00', 240000.00, 'WEEKEND'), (4, 13, '17:00', '23:00', 290000.00, 'WEEKEND'),
-- ft=14: Bóng chuyền CLC
(4, 14, '06:00', '12:00', 200000.00, 'WEEKDAY'), (4, 14, '13:00', '17:00', 240000.00, 'WEEKDAY'), (4, 14, '17:00', '23:00', 280000.00, 'WEEKDAY'),
(4, 14, '06:00', '12:00', 260000.00, 'WEEKEND'), (4, 14, '13:00', '17:00', 300000.00, 'WEEKEND'), (4, 14, '17:00', '23:00', 350000.00, 'WEEKEND'),
-- ft=16: Tennis ngoài trời
(4, 16, '06:00', '12:00', 120000.00, 'WEEKDAY'), (4, 16, '13:00', '17:00', 150000.00, 'WEEKDAY'), (4, 16, '17:00', '23:00', 180000.00, 'WEEKDAY'),
(4, 16, '06:00', '12:00', 160000.00, 'WEEKEND'), (4, 16, '13:00', '17:00', 190000.00, 'WEEKEND'), (4, 16, '17:00', '23:00', 220000.00, 'WEEKEND'),
-- ft=17: Tennis mái che
(4, 17, '06:00', '12:00', 160000.00, 'WEEKDAY'), (4, 17, '13:00', '17:00', 190000.00, 'WEEKDAY'), (4, 17, '17:00', '23:00', 230000.00, 'WEEKDAY'),
(4, 17, '06:00', '12:00', 200000.00, 'WEEKEND'), (4, 17, '13:00', '17:00', 240000.00, 'WEEKEND'), (4, 17, '17:00', '23:00', 280000.00, 'WEEKEND'),
-- ft=22: Đá cầu tiêu chuẩn
(4, 22, '06:00', '12:00',  40000.00, 'WEEKDAY'), (4, 22, '13:00', '17:00',  55000.00, 'WEEKDAY'), (4, 22, '17:00', '23:00',  70000.00, 'WEEKDAY'),
(4, 22, '06:00', '12:00',  55000.00, 'WEEKEND'), (4, 22, '13:00', '17:00',  70000.00, 'WEEKEND'), (4, 22, '17:00', '23:00',  85000.00, 'WEEKEND'),
-- ft=23: Đá cầu CLC
(4, 23, '06:00', '12:00',  55000.00, 'WEEKDAY'), (4, 23, '13:00', '17:00',  70000.00, 'WEEKDAY'), (4, 23, '17:00', '23:00',  90000.00, 'WEEKDAY'),
(4, 23, '06:00', '12:00',  70000.00, 'WEEKEND'), (4, 23, '13:00', '17:00',  90000.00, 'WEEKEND'), (4, 23, '17:00', '23:00', 110000.00, 'WEEKEND');

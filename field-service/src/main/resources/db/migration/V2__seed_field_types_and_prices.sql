-- field-service: V2__seed_field_types_and_prices.sql
-- Thêm field types cho Bóng đá (sport_id=1)
INSERT INTO field_types (sport_id, name, player_capacity) VALUES
    (1, 'Sân 5 người', 10),
    (1, 'Sân 7 người', 14),
    (1, 'Sân 11 người', 22);

-- Thêm field types cho Cầu lông (sport_id=2)
INSERT INTO field_types (sport_id, name, player_capacity) VALUES
    (2, 'Sân cầu lông đơn', 2),
    (2, 'Sân cầu lông đôi', 4);

-- Thêm fields tại Location 1 (Hà Nội)
INSERT INTO fields (location_id, field_type_id, name, status) VALUES
    (1, 1, 'Sân 5A', 'AVAILABLE'),
    (1, 1, 'Sân 5B', 'AVAILABLE'),
    (1, 2, 'Sân 7A', 'AVAILABLE'),
    (1, 3, 'Sân 11A', 'AVAILABLE');

-- Thêm fields tại Location 2 (Bắc Ninh)
INSERT INTO fields (location_id, field_type_id, name, status) VALUES
    (2, 1, 'Sân 5C', 'AVAILABLE'),
    (2, 2, 'Sân 7B', 'AVAILABLE');

-- Bảng giá cho Sân 5 người (field_type_id=1) tại Location 1
-- Buổi sáng ngày thường: 06:00-12:00 - 100.000đ/giờ
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 1, '06:00:00', '12:00:00', 100000.00, 'WEEKDAY'),
    (1, 1, '12:00:00', '18:00:00', 120000.00, 'WEEKDAY'),
    (1, 1, '18:00:00', '22:00:00', 150000.00, 'WEEKDAY'),
    (1, 1, '06:00:00', '12:00:00', 120000.00, 'WEEKEND'),
    (1, 1, '12:00:00', '18:00:00', 140000.00, 'WEEKEND'),
    (1, 1, '18:00:00', '22:00:00', 180000.00, 'WEEKEND');

-- Bảng giá cho Sân 7 người (field_type_id=2) tại Location 1
INSERT INTO prices (location_id, field_type_id, start_time, end_time, price, day_type) VALUES
    (1, 2, '06:00:00', '12:00:00', 200000.00, 'WEEKDAY'),
    (1, 2, '12:00:00', '18:00:00', 230000.00, 'WEEKDAY'),
    (1, 2, '18:00:00', '22:00:00', 280000.00, 'WEEKDAY'),
    (1, 2, '06:00:00', '12:00:00', 240000.00, 'WEEKEND'),
    (1, 2, '12:00:00', '18:00:00', 270000.00, 'WEEKEND'),
    (1, 2, '18:00:00', '22:00:00', 320000.00, 'WEEKEND');

-- field-service: V1__init_field.sql
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
    FOREIGN KEY (location_id) REFERENCES locations(id),
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
    FOREIGN KEY (location_id) REFERENCES locations(id),
    FOREIGN KEY (field_type_id) REFERENCES field_types(id)
);

CREATE INDEX idx_fields_location   ON fields(location_id);
CREATE INDEX idx_prices_location   ON prices(location_id, field_type_id);

-- Sample data
INSERT INTO sports (name, slug) VALUES ('Bóng đá', 'bong-da'), ('Cầu lông', 'cau-long'), ('Bóng rổ', 'bong-ro');
INSERT INTO locations (name, address, region, hotline) VALUES
    ('Sportify Hà Nội', '123 Đường Láng, Đống Đa, Hà Nội', 'Hà Nội', '0901234567'),
    ('Sportify Bắc Ninh', '45 Đường Lý Thái Tổ, TP Bắc Ninh', 'Bắc Ninh', '0907654321');

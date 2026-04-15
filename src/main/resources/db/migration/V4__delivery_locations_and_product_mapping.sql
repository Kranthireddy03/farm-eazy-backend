-- Flyway migration: delivery locations and product location mapping

CREATE TABLE IF NOT EXISTS delivery_location (
    id BIGINT NOT NULL AUTO_INCREMENT,
    location_name VARCHAR(255) NOT NULL,
    city VARCHAR(120) NOT NULL,
    state VARCHAR(120) NOT NULL,
    postal_code VARCHAR(20) NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    radius_km DECIMAL(8, 2) NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    notes TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_delivery_location_active (active),
    INDEX idx_delivery_location_city_state (city, state),
    INDEX idx_delivery_location_postal_code (postal_code)
);

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS delivery_location_id BIGINT NULL;

CREATE INDEX IF NOT EXISTS idx_product_delivery_location_id ON product (delivery_location_id);

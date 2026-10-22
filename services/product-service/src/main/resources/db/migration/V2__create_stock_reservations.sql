-- V2: Stock reservation tables and non-negative stock constraint

ALTER TABLE products 
    ADD CONSTRAINT chk_products_stock_non_negative CHECK (stock_quantity >= 0);

CREATE TABLE IF NOT EXISTS stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    reservation_id VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_status ON stock_reservations(status);

CREATE TABLE IF NOT EXISTS stock_reservation_items (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL REFERENCES stock_reservations(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    CONSTRAINT chk_reservation_item_quantity CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_stock_res_items_reservation ON stock_reservation_items(reservation_id);
CREATE INDEX IF NOT EXISTS idx_stock_res_items_product ON stock_reservation_items(product_id);

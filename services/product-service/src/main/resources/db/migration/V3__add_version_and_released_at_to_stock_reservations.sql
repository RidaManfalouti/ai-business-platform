-- V3: Add optimistic lock version and released_at timestamp to stock_reservations
ALTER TABLE stock_reservations ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE stock_reservations ADD COLUMN IF NOT EXISTS released_at TIMESTAMP WITHOUT TIME ZONE;

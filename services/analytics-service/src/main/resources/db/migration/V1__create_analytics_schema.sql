CREATE TABLE IF NOT EXISTS analytics_snapshots (
    id BIGSERIAL PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    total_revenue NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    completed_orders BIGINT NOT NULL DEFAULT 0,
    total_orders BIGINT NOT NULL DEFAULT 0,
    total_customers BIGINT NOT NULL DEFAULT 0,
    total_products BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_analytics_snapshots_date ON analytics_snapshots(snapshot_date);

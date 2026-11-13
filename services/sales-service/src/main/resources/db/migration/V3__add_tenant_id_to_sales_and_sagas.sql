-- V3: Multi-tenant isolation for sales and sagas
-- Deterministic legacy tenant UUID: 00000000-0000-0000-0000-000000000001

-- 1. Sales table
ALTER TABLE sales
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

UPDATE sales
SET tenant_id = '00000000-0000-0000-0000-000000000001'
WHERE tenant_id IS NULL;

ALTER TABLE sales
    ALTER COLUMN tenant_id SET NOT NULL;

DROP INDEX IF EXISTS idx_sales_reference;
CREATE UNIQUE INDEX IF NOT EXISTS uq_sales_tenant_reference ON sales (tenant_id, reference);
CREATE INDEX IF NOT EXISTS idx_sales_tenant_id ON sales (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sales_tenant_created_at ON sales (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_sales_tenant_status ON sales (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_sales_tenant_customer ON sales (tenant_id, customer_id);

-- 2. Sale items table
ALTER TABLE sale_items
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

UPDATE sale_items
SET tenant_id = '00000000-0000-0000-0000-000000000001'
WHERE tenant_id IS NULL;

ALTER TABLE sale_items
    ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sale_items_tenant_id ON sale_items (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_tenant_product ON sale_items (tenant_id, product_id);

-- 3. Sagas table
ALTER TABLE sagas
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

UPDATE sagas
SET tenant_id = '00000000-0000-0000-0000-000000000001'
WHERE tenant_id IS NULL;

ALTER TABLE sagas
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE sagas
    DROP CONSTRAINT IF EXISTS sagas_saga_id_key;

ALTER TABLE sagas
    ADD CONSTRAINT uq_sagas_tenant_saga_id UNIQUE (tenant_id, saga_id);

CREATE INDEX IF NOT EXISTS idx_sagas_tenant_id ON sagas (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sagas_tenant_sale_ref ON sagas (tenant_id, sale_reference);

-- 4. Saga events table
ALTER TABLE saga_events
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

UPDATE saga_events
SET tenant_id = '00000000-0000-0000-0000-000000000001'
WHERE tenant_id IS NULL;

ALTER TABLE saga_events
    ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_saga_events_tenant_id ON saga_events (tenant_id);

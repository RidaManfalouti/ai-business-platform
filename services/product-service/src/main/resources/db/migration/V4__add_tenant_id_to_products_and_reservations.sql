-- V4: Add tenant isolation to products and stock reservations

-- 1. Add tenant_id to products
ALTER TABLE products ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- 2. Backfill existing records with the deterministic legacy tenant ID
UPDATE products 
SET tenant_id = '00000000-0000-0000-0000-000000000001' 
WHERE tenant_id IS NULL;

-- 3. Enforce NOT NULL on tenant_id
ALTER TABLE products ALTER COLUMN tenant_id SET NOT NULL;

-- 4. Drop global uniqueness on name and replace with composite (tenant_id, name)
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_name_key;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_products_tenant_name'
    ) THEN
        ALTER TABLE products ADD CONSTRAINT uq_products_tenant_name UNIQUE (tenant_id, name);
    END IF;
END $$;

-- 5. Add indexes for tenant-scoped catalog browsing
CREATE INDEX IF NOT EXISTS idx_products_tenant_category ON products(tenant_id, category);
CREATE INDEX IF NOT EXISTS idx_products_tenant_id ON products(tenant_id, id);

-- 6. Add tenant_id to stock_reservations
ALTER TABLE stock_reservations ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- 7. Backfill existing reservations
UPDATE stock_reservations 
SET tenant_id = '00000000-0000-0000-0000-000000000001' 
WHERE tenant_id IS NULL;

-- 8. Enforce NOT NULL on tenant_id
ALTER TABLE stock_reservations ALTER COLUMN tenant_id SET NOT NULL;

-- 9. Add composite index on (tenant_id, reservation_id)
CREATE INDEX IF NOT EXISTS idx_stock_res_tenant ON stock_reservations(tenant_id, reservation_id);

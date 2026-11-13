-- Migration V2: Add tenant_id to customers table for multi-tenant isolation
-- Deterministic legacy tenant UUID: 00000000-0000-0000-0000-000000000001

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- Backfill existing legacy customer records
UPDATE customers
SET tenant_id = '00000000-0000-0000-0000-000000000001'
WHERE tenant_id IS NULL;

-- Enforce NOT NULL constraint
ALTER TABLE customers
    ALTER COLUMN tenant_id SET NOT NULL;

-- Drop global unique constraint on email
ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS customers_email_key;

-- Add tenant-scoped composite unique constraint
ALTER TABLE customers
    ADD CONSTRAINT uq_customers_tenant_email UNIQUE (tenant_id, email);

-- Create tenant-scoped indexes
CREATE INDEX IF NOT EXISTS idx_customers_tenant_id ON customers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_customers_tenant_name ON customers(tenant_id, last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_customers_tenant_company ON customers(tenant_id, company);

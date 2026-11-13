-- Migration V2: Add tenant_id to analytics_snapshots for multi-tenant isolation
ALTER TABLE analytics_snapshots
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- Backfill legacy records with default single-tenant company ID
UPDATE analytics_snapshots
SET tenant_id = '00000000-0000-0000-0000-000000000001'
WHERE tenant_id IS NULL;

-- Enforce NOT NULL constraint
ALTER TABLE analytics_snapshots
    ALTER COLUMN tenant_id SET NOT NULL;

-- Drop obsolete global index and create tenant-scoped composite indexes
DROP INDEX IF EXISTS idx_analytics_snapshots_date;
CREATE UNIQUE INDEX IF NOT EXISTS uq_analytics_snapshots_tenant_date ON analytics_snapshots(tenant_id, snapshot_date);
CREATE INDEX IF NOT EXISTS idx_analytics_snapshots_tenant_id ON analytics_snapshots(tenant_id);

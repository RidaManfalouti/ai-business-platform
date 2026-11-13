-- V5: Fix cross-tenant stock reservation unique constraint (SEC-MT-03)
-- Drop global uniqueness on reservation_id so reservation IDs are scoped strictly per tenant

ALTER TABLE stock_reservations DROP CONSTRAINT IF EXISTS stock_reservations_reservation_id_key;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_stock_reservations_tenant_reservation_id'
    ) THEN
        ALTER TABLE stock_reservations ADD CONSTRAINT uq_stock_reservations_tenant_reservation_id UNIQUE (tenant_id, reservation_id);
    END IF;
END $$;

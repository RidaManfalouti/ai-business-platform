-- V2: Create tenants and memberships tables and seed deterministic legacy tenant

CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE INDEX IF NOT EXISTS idx_tenants_slug ON tenants(slug);
CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants(status);

CREATE TABLE IF NOT EXISTS memberships (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uq_memberships_tenant_user UNIQUE (tenant_id, user_id),
    CONSTRAINT chk_membership_role CHECK (role IN ('OWNER', 'ADMIN', 'MANAGER', 'SELLER', 'VIEWER')),
    CONSTRAINT chk_membership_status CHECK (status IN ('ACTIVE', 'INVITED', 'SUSPENDED'))
);

CREATE INDEX IF NOT EXISTS idx_memberships_user_id ON memberships(user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_tenant_id ON memberships(tenant_id);

-- Seed deterministic legacy tenant
INSERT INTO tenants (id, name, slug, status, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Entreprise Initiale (Legacy Platform)',
    'default-company',
    'ACTIVE',
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- Backfill legacy memberships for existing users:
-- ADMIN users become OWNER of legacy tenant, other users become MANAGER
INSERT INTO memberships (id, tenant_id, user_id, role, status, created_at)
SELECT 
    gen_random_uuid(),
    '00000000-0000-0000-0000-000000000001',
    u.id,
    CASE WHEN u.role = 'ADMIN' THEN 'OWNER' ELSE 'MANAGER' END,
    'ACTIVE',
    NOW()
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM memberships m 
    WHERE m.tenant_id = '00000000-0000-0000-0000-000000000001' AND m.user_id = u.id
);

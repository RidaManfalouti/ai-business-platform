-- V2: Persistent Saga orchestration and audit history tables

CREATE TABLE sagas (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(64) NOT NULL UNIQUE,
    saga_type VARCHAR(30) NOT NULL,
    sale_id BIGINT,
    sale_reference VARCHAR(64) NOT NULL,
    reservation_id VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    last_attempt_at TIMESTAMP WITHOUT TIME ZONE,
    next_retry_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    last_error VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_sagas_status_next_retry ON sagas(status, next_retry_at);
CREATE INDEX idx_sagas_sale_reference ON sagas(sale_reference);
CREATE INDEX idx_sagas_sale_id ON sagas(sale_id);
CREATE INDEX idx_sagas_created_at ON sagas(created_at);

CREATE TABLE saga_events (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(64) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    message VARCHAR(1000),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saga_events_saga_id ON saga_events(saga_id);
CREATE INDEX idx_saga_events_created_at ON saga_events(created_at);

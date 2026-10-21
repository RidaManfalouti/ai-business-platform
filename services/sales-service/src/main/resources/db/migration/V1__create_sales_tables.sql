CREATE TABLE sales (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(64) NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT chk_sales_total_amount CHECK (total_amount >= 0)
);

CREATE UNIQUE INDEX idx_sales_reference ON sales (reference);
CREATE INDEX idx_sales_customer_id ON sales (customer_id);
CREATE INDEX idx_sales_status ON sales (status);
CREATE INDEX idx_sales_created_at ON sales (created_at);

CREATE TABLE sale_items (
    id BIGSERIAL PRIMARY KEY,
    sale_id BIGINT NOT NULL REFERENCES sales (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    product_name_snapshot VARCHAR(150) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    line_total NUMERIC(12, 2) NOT NULL CHECK (line_total >= 0)
);

CREATE INDEX idx_sale_items_sale_id ON sale_items (sale_id);
CREATE INDEX idx_sale_items_product_id ON sale_items (product_id);

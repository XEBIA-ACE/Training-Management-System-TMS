-- US-002 (FR-US-002-001..005): orders created via POST /api/v1/orders.
CREATE TABLE orders (
    id          UUID        PRIMARY KEY,
    customer_id UUID        NOT NULL REFERENCES customer (id),
    product_id  UUID        NOT NULL REFERENCES product (id),
    quantity    INTEGER     NOT NULL CHECK (quantity > 0),
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_orders_customer ON orders (customer_id);

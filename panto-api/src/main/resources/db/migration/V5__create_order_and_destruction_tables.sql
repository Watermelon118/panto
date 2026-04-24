CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'ROLLED_BACK')),
    subtotal_amount DECIMAL(12, 2) NOT NULL CHECK (subtotal_amount >= 0),
    gst_amount DECIMAL(12, 2) NOT NULL CHECK (gst_amount >= 0),
    total_amount DECIMAL(12, 2) NOT NULL CHECK (total_amount >= 0),
    rolled_back_at TIMESTAMPTZ,
    rolled_back_by BIGINT REFERENCES users(id),
    rollback_reason TEXT,
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    CHECK (total_amount = subtotal_amount + gst_amount),
    CHECK (
        (status = 'ACTIVE' AND rolled_back_at IS NULL AND rolled_back_by IS NULL)
        OR
        (status = 'ROLLED_BACK' AND rolled_back_at IS NOT NULL AND rolled_back_by IS NOT NULL)
    )
);

CREATE INDEX idx_orders_customer_time ON orders(customer_id, created_at DESC);
CREATE INDEX idx_orders_status_time ON orders(status, created_at DESC);
CREATE INDEX idx_orders_date_range ON orders(created_at DESC) WHERE status = 'ACTIVE';
CREATE INDEX idx_orders_number ON orders(order_number);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    batch_id BIGINT NOT NULL REFERENCES batches(id),
    product_name_snapshot VARCHAR(200) NOT NULL,
    product_sku_snapshot VARCHAR(50) NOT NULL,
    product_unit_snapshot VARCHAR(20) NOT NULL,
    product_spec_snapshot VARCHAR(100),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12, 2) NOT NULL CHECK (unit_price >= 0),
    subtotal DECIMAL(12, 2) NOT NULL CHECK (subtotal >= 0),
    is_gst_applicable BOOLEAN NOT NULL,
    gst_amount DECIMAL(12, 2) NOT NULL CHECK (gst_amount >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (subtotal = quantity * unit_price)
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
CREATE INDEX idx_order_items_batch ON order_items(batch_id);

CREATE TABLE destructions (
    id BIGSERIAL PRIMARY KEY,
    destruction_number VARCHAR(30) NOT NULL UNIQUE,
    batch_id BIGINT NOT NULL REFERENCES batches(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    inventory_transaction_id BIGINT NOT NULL REFERENCES inventory_transactions(id),
    quantity_destroyed INTEGER NOT NULL CHECK (quantity_destroyed > 0),
    purchase_unit_price_snapshot DECIMAL(12, 2) NOT NULL,
    loss_amount DECIMAL(12, 2) NOT NULL CHECK (loss_amount >= 0),
    reason TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    CHECK (loss_amount = quantity_destroyed * purchase_unit_price_snapshot)
);

CREATE INDEX idx_destructions_batch ON destructions(batch_id);
CREATE INDEX idx_destructions_product_time ON destructions(product_id, created_at DESC);
CREATE INDEX idx_destructions_time ON destructions(created_at DESC);

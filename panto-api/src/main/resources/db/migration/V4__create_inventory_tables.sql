CREATE TABLE inbound_records (
    id BIGSERIAL PRIMARY KEY,
    inbound_number VARCHAR(30) NOT NULL UNIQUE,
    inbound_date DATE NOT NULL,
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_inbound_records_date ON inbound_records(inbound_date DESC);
CREATE INDEX idx_inbound_records_number ON inbound_records(inbound_number);

CREATE TABLE inbound_items (
    id BIGSERIAL PRIMARY KEY,
    inbound_record_id BIGINT NOT NULL REFERENCES inbound_records(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    batch_number VARCHAR(50) NOT NULL,
    expiry_date DATE NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    purchase_unit_price DECIMAL(12, 2) NOT NULL CHECK (purchase_unit_price >= 0),
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_inbound_items_record ON inbound_items(inbound_record_id);
CREATE INDEX idx_inbound_items_product ON inbound_items(product_id);

CREATE TABLE batches (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    inbound_item_id BIGINT NOT NULL REFERENCES inbound_items(id),
    batch_number VARCHAR(50) NOT NULL,
    arrival_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    quantity_received INTEGER NOT NULL CHECK (quantity_received > 0),
    quantity_remaining INTEGER NOT NULL CHECK (quantity_remaining >= 0),
    purchase_unit_price DECIMAL(12, 2) NOT NULL CHECK (purchase_unit_price >= 0),
    expiry_status VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
        CHECK (expiry_status IN ('NORMAL', 'EXPIRING_SOON', 'EXPIRED')),
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    CHECK (quantity_remaining <= quantity_received),
    CHECK (expiry_date >= arrival_date),
    UNIQUE (product_id, batch_number)
);

CREATE INDEX idx_batches_product_available
    ON batches(product_id, expiry_date) WHERE quantity_remaining > 0;
CREATE INDEX idx_batches_expiry_status
    ON batches(expiry_status, expiry_date) WHERE quantity_remaining > 0;
CREATE INDEX idx_batches_expiry_date
    ON batches(expiry_date) WHERE quantity_remaining > 0;

CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES batches(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    transaction_type VARCHAR(20) NOT NULL
        CHECK (transaction_type IN ('IN', 'OUT', 'ROLLBACK', 'DESTROY', 'ADJUST')),
    quantity_delta INTEGER NOT NULL CHECK (quantity_delta != 0),
    quantity_before INTEGER NOT NULL CHECK (quantity_before >= 0),
    quantity_after INTEGER NOT NULL CHECK (quantity_after >= 0),
    related_document_type VARCHAR(20),
    related_document_id BIGINT,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    CHECK (quantity_after = quantity_before + quantity_delta)
);

CREATE INDEX idx_inv_tx_batch_time ON inventory_transactions(batch_id, created_at DESC);
CREATE INDEX idx_inv_tx_product_time ON inventory_transactions(product_id, created_at DESC);
CREATE INDEX idx_inv_tx_type_time ON inventory_transactions(transaction_type, created_at DESC);
CREATE INDEX idx_inv_tx_related ON inventory_transactions(related_document_type, related_document_id);

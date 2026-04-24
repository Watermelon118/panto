CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    specification VARCHAR(100),
    unit VARCHAR(20) NOT NULL,
    reference_purchase_price DECIMAL(12, 2) NOT NULL CHECK (reference_purchase_price >= 0),
    reference_sale_price DECIMAL(12, 2) NOT NULL CHECK (reference_sale_price >= 0),
    safety_stock INTEGER NOT NULL DEFAULT 0 CHECK (safety_stock >= 0),
    is_gst_applicable BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_products_sku ON products(sku) WHERE is_active = TRUE;
CREATE INDEX idx_products_category ON products(category) WHERE is_active = TRUE;
CREATE INDEX idx_products_name_trgm ON products USING GIN (name gin_trgm_ops);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(30),
    email VARCHAR(100),
    address TEXT,
    gst_number VARCHAR(20),
    remarks TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_customers_company_name ON customers(company_name) WHERE is_active = TRUE;
CREATE INDEX idx_customers_phone ON customers(phone) WHERE is_active = TRUE AND phone IS NOT NULL;
CREATE INDEX idx_customers_name_trgm ON customers USING GIN (company_name gin_trgm_ops);

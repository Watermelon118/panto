markdown# Panto — Design Document

**Project**: Panto, a food warehouse management system  
**Author**: Louis Wang (Shuaijie Wang)  
**Created**: 2026-04-24  
**Last Updated**: 2026-04-25  
**Status**: Draft  

---

## Version History

| Version | Date       | Author | Changes                                  |
|---------|------------|--------|------------------------------------------|
| 0.1     | 2026-04-24 | Louis  | Initial draft – Architecture section     |
| 0.2     | 2026-04-25 | Louis  | Align tech stack versions with current repository |

---

## 1. Background & Problem Statement

Panto is a single-warehouse management system designed for small food wholesale businesses in New Zealand. The reference business scenario is a B2B food distributor serving commercial customers such as restaurants and companies.

**Current pain points in the reference business**:
- Orders are taken informally via instant messaging; no systematic record of what was sold to whom
- Inventory and expiry tracking rely on manual spreadsheets, which are error-prone
- No audit trail for who modified what data — accountability is weak
- Accountants need to manually compile sales reports from chat records

**Panto aims to solve these by providing**:
- A simple but complete inventory system with batch and expiry tracking
- Role-based order entry with automatic invoice generation
- Complete audit trail for every data modification
- One-click export for accounting and auditing

---

## 2. Goals & Non-Goals

### 2.1 Goals

1. Manage food inbound stock with batch numbers and expiry dates
2. Manage B2B customer information
3. Allow authorised staff to place orders on behalf of customers and generate standard NZ-compliant invoices (Customer Copy + Office Copy)
4. Automatically warn users about expiring stock; allow manual destruction with cost tracking
5. Record every data modification with operator, timestamp, and before/after snapshot
6. Allow the accountant to export sales and loss reports by date range

### 2.2 Non-Goals (explicitly excluded)

- Online payment integration (WeChat Pay, Stripe, etc.)
- Order status workflow (paid / shipped / delivered)
- Customer credit / monthly account settlement (maintained externally in Excel)
- Multi-warehouse, logistics, or delivery routing
- Customer self-service ordering
- Mobile application (web-only for v1)
- User self-registration (manager creates accounts)
- Supplier management (inbound records batch + expiry only)

---

## 3. User Roles & Permissions

Four roles, with strict separation of concerns to enforce accountability.

| Function                       | Manager | Warehouse | Marketing | Accountant |
|--------------------------------|---------|-----------|-----------|------------|
| User management                | ✓       |           |           |            |
| Product management (CRUD)      | ✓       | ✓         |           |            |
| Inbound (create / modify)      | ✓       | ✓         |           |            |
| Customer management (CRUD)     | ✓       |           | ✓         |            |
| Create sales order / invoice   | ✓       | ✓         | ✓         |            |
| Rollback order                 | ✓       | ✓         | ✓         |            |
| Destroy expired stock          | ✓       | ✓         |           |            |
| View expiry / stock warnings   | ✓       | ✓         | ✓         | ✓          |
| View dashboard                 | ✓       | ✓         | ✓         | ✓          |
| View inventory (read-only)     | ✓       | ✓         | ✓         | ✓          |
| Export financial reports       | ✓       |           |           | ✓          |
| View audit log                 | ✓       |           |           |            |

**The accountant is strictly read-only** — they can view and export, but cannot modify any business data.

---

## 4. System Architecture

### 4.1 High-Level Architecture

Panto is a classic three-tier web application: a React single-page app in the browser, a Spring Boot REST API on the server, and PostgreSQL for persistence. Nginx sits in front as a reverse proxy.

The following diagram shows the request flow:

~~~
+-------------------------------------------------------------+
|                   Browser (Chrome / Edge)                   |
|                                                             |
|  panto-web (React SPA, built by Vite)                       |
|    - Access Token stored in memory (Zustand store)          |
|    - Refresh Token stored in httpOnly + Secure cookie       |
+-------------------------------------------------------------+
                            |
                 HTTPS (REST API calls)
                 Authorization: Bearer <Access Token>
                            |
                            v
+-------------------------------------------------------------+
|              Nginx reverse proxy (port 443)                 |
|    - SSL termination (Let's Encrypt certificate)            |
|    - Route "/"      -> panto-web static files               |
|    - Route "/api/*" -> panto-api backend                    |
+-------------------------------------------------------------+
                            |
                            v
+-------------------------------------------------------------+
|                panto-api (Spring Boot 3.5.x)                |
|                                                             |
|   Security Filter Chain    (JWT validation + RBAC)          |
|   Controller layer         (REST endpoints)                 |
|   Service layer            (business logic, @Transactional) |
|   Repository layer         (Spring Data JPA)                |
|                                                             |
|   Cross-cutting concerns:                                   |
|     - Scheduled jobs   (@Scheduled: daily expiry scan)      |
|     - AOP aspects      (@Auditable -> audit log writer)     |
+-------------------------------------------------------------+
                            |
                           JDBC
                            |
                            v
+-------------------------------------------------------------+
|                      PostgreSQL 15                          |
|   - Business tables (users, products, batches, orders, ...) |
|   - Audit log table (append-only, separate from business)   |
+-------------------------------------------------------------+
~~~

### 4.2 Deployment Topology

All services run on a single AWS EC2 instance in Sydney (`ap-southeast-2`), orchestrated by Docker Compose. This keeps the setup simple while still being containerised and portable.

~~~
                         Internet
                            |
                            v
                 AWS Route 53 (DNS: panto.example.com)
                            |
                            v
   +----------------------------------------------------+
   |     AWS EC2 Instance  (t3.small, Sydney region)    |
   |                                                    |
   |   Docker Compose orchestrates four containers:     |
   |                                                    |
   |     +----------+   +------------+   +----------+   |
   |     |  nginx   |   |  panto-api |   |panto-web |   |
   |     | (proxy)  |-->|(Spring Boot|   | (static  |   |
   |     |  :443    |   |   :8080)   |   |  Nginx)  |   |
   |     +----------+   +------------+   +----------+   |
   |                          |                         |
   |                          v                         |
   |                    +------------+                  |
   |                    | postgres   |                  |
   |                    |  :5432     |                  |
   |                    +------------+                  |
   |                          |                         |
   |                          v                         |
   |                   Named Docker volume              |
   |                   (persistent data)                |
   +----------------------------------------------------+

   SSL certificates: Let's Encrypt, auto-renewed by certbot
~~~

**Rationale for single-node deployment**: given the scale (fewer than 10 concurrent internal users, one warehouse), distributed deployment would add cost and operational burden without meaningful benefit. Containerisation keeps the door open for future migration to ECS or Kubernetes if the business scales.

### 4.3 Key Technical Decisions

| # | Area | Choice | Rationale |
|---|------|--------|-----------|
| 1 | Cloud provider | AWS EC2 | Most common requirement in NZ job market; free tier covers this project |
| 2 | JWT storage | Access Token in memory, Refresh Token in httpOnly cookie | Defends against both XSS (JS cannot read httpOnly cookies) and CSRF (Access Token is not sent automatically by the browser) |
| 3 | Scheduled jobs | Spring `@Scheduled` | Zero dependency; single-instance deployment has no duplicate execution problem. Would migrate to ShedLock or Quartz if scaled to multiple instances |
| 4 | Audit logging | AOP aspect + custom `@Auditable` annotation | Keeps business code clean; avoids scattered `auditService.log(...)` calls throughout services |
| 5 | Frontend state | TanStack Query (server state) + Zustand (client state) | Separation of concerns; avoids the Redux anti-pattern of funnelling everything through one global store |
| 6 | API response shape | `Result<T> { code, message, data }` | Uniform contract; frontend handles success and error by checking the `code` field |
| 7 | Exception handling | Global `@RestControllerAdvice` + custom `BusinessException` hierarchy | Centralised; unknown errors get a trace ID for support and log correlation |
| 8 | Password storage | BCrypt, cost factor 12 | Industry-standard password hashing; MD5/SHA-family are unsafe for passwords because they are too fast |

---

## 5. Data Model

### 5.1 Overview

Panto's data model has 12 tables organised into 5 groups:

- **User & Auth**: `users`, `login_attempts`
- **Master Data**: `products`, `customers`
- **Inventory & Batch**: `batches`, `inbound_records`, `inbound_items`, `inventory_transactions`
- **Transaction**: `orders`, `order_items`, `destructions`
- **System**: `audit_logs`

### 5.2 Entity Relationships (summary)

- `users` is referenced by almost every table as `created_by` / `updated_by` — all changes are traceable to a person
- `products` → `batches` (1:N) — one product has many stock batches
- `inbound_records` → `inbound_items` → `batches` (chain 1:N → 1:1) — one inbound record creates one batch per line item
- `customers` → `orders` (1:N) — one customer has many orders
- `orders` → `order_items` (1:N) — one order has many line items
- `order_items` → `batches` (N:1) — each line item records the exact batch it deducted from
- `batches` → `inventory_transactions` (1:N) — every stock movement on a batch is logged
- `batches` → `destructions` (1:N) — a batch can be partially or fully destroyed multiple times
- `destructions` → `inventory_transactions` (1:1) — each destruction event links to its stock movement record

### 5.3 Conventions

All business tables follow these rules:

- Primary key: `id BIGSERIAL`
- Timestamps: `created_at`, `updated_at` are `TIMESTAMP WITH TIME ZONE`
- Operator: `created_by`, `updated_by` reference `users(id)`
- Soft delete: business entities use `is_active BOOLEAN` or a `status` column
- Money: `DECIMAL(12, 2)` (never FLOAT)
- Booleans: prefixed with `is_` (e.g., `is_active`)
- Immutable tables (`inventory_transactions`, `audit_logs`, `destructions`) have `created_at` + `created_by` only — no update columns

### 5.4 Table Schemas

#### `users`

```sql
CREATE TABLE users (
    id                      BIGSERIAL PRIMARY KEY,
    username                VARCHAR(50)  NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL CHECK (LENGTH(password_hash) >= 60),
    full_name               VARCHAR(100) NOT NULL,
    email                   VARCHAR(100) UNIQUE,
    role                    VARCHAR(20)  NOT NULL 
                            CHECK (role IN ('ADMIN', 'WAREHOUSE', 'MARKETING', 'ACCOUNTANT')),
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password    BOOLEAN      NOT NULL DEFAULT TRUE,
    locked_until            TIMESTAMP WITH TIME ZONE,
    last_login_at           TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT REFERENCES users(id),
    updated_by              BIGINT REFERENCES users(id)
);

CREATE INDEX idx_users_username ON users(username) WHERE is_active = TRUE;
CREATE INDEX idx_users_role ON users(role) WHERE is_active = TRUE;
```

#### `login_attempts`

```sql
CREATE TABLE login_attempts (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    ip_address      VARCHAR(45),
    success         BOOLEAN      NOT NULL,
    failure_reason  VARCHAR(50),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_login_attempts_username_time ON login_attempts(username, created_at DESC);
CREATE INDEX idx_login_attempts_ip_time ON login_attempts(ip_address, created_at DESC);
```

Retention: records older than 90 days are deleted by a daily scheduled job.

#### `products`

```sql
CREATE TABLE products (
    id                          BIGSERIAL PRIMARY KEY,
    sku                         VARCHAR(50)  NOT NULL UNIQUE,
    name                        VARCHAR(200) NOT NULL,
    category                    VARCHAR(50)  NOT NULL,
    specification               VARCHAR(100),
    unit                        VARCHAR(20)  NOT NULL,
    reference_purchase_price    DECIMAL(12, 2) NOT NULL CHECK (reference_purchase_price >= 0),
    reference_sale_price        DECIMAL(12, 2) NOT NULL CHECK (reference_sale_price >= 0),
    safety_stock                INTEGER      NOT NULL DEFAULT 0 CHECK (safety_stock >= 0),
    is_gst_applicable           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active                   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  BIGINT NOT NULL REFERENCES users(id),
    updated_by                  BIGINT NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_products_sku ON products(sku) WHERE is_active = TRUE;
CREATE INDEX idx_products_category ON products(category) WHERE is_active = TRUE;
CREATE INDEX idx_products_name_trgm ON products USING GIN (name gin_trgm_ops);
```

Note: `pg_trgm` extension required for fuzzy-search index.

#### `customers`

```sql
CREATE TABLE customers (
    id              BIGSERIAL PRIMARY KEY,
    company_name    VARCHAR(200) NOT NULL,
    contact_person  VARCHAR(100),
    phone           VARCHAR(30),
    email           VARCHAR(100),
    address         TEXT,
    gst_number      VARCHAR(20),
    remarks         TEXT,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT NOT NULL REFERENCES users(id),
    updated_by      BIGINT NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_customers_company_name ON customers(company_name) WHERE is_active = TRUE;
CREATE INDEX idx_customers_phone ON customers(phone) WHERE is_active = TRUE AND phone IS NOT NULL;
CREATE INDEX idx_customers_name_trgm ON customers USING GIN (company_name gin_trgm_ops);
```

#### `inbound_records`

```sql
CREATE TABLE inbound_records (
    id                  BIGSERIAL PRIMARY KEY,
    inbound_number      VARCHAR(30)  NOT NULL UNIQUE,
    inbound_date        DATE         NOT NULL,
    remarks             TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT NOT NULL REFERENCES users(id),
    updated_by          BIGINT NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_inbound_records_date ON inbound_records(inbound_date DESC);
CREATE INDEX idx_inbound_records_number ON inbound_records(inbound_number);
```

Format for `inbound_number`: `IN-YYYYMMDD-NNN` (generated by application).

#### `inbound_items`

```sql
CREATE TABLE inbound_items (
    id                      BIGSERIAL PRIMARY KEY,
    inbound_record_id       BIGINT NOT NULL REFERENCES inbound_records(id) ON DELETE CASCADE,
    product_id              BIGINT NOT NULL REFERENCES products(id),
    batch_number            VARCHAR(50)  NOT NULL,
    expiry_date             DATE NOT NULL,
    quantity                INTEGER NOT NULL CHECK (quantity > 0),
    purchase_unit_price     DECIMAL(12, 2) NOT NULL CHECK (purchase_unit_price >= 0),
    remarks                 TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT NOT NULL REFERENCES users(id),
    updated_by              BIGINT NOT NULL REFERENCES users(id)
);

CREATE INDEX idx_inbound_items_record ON inbound_items(inbound_record_id);
CREATE INDEX idx_inbound_items_product ON inbound_items(product_id);
```

Format for `batch_number`: `{SKU}-{YYYYMMDD}-{NNN}` (generated by application).

#### `batches`

```sql
CREATE TABLE batches (
    id                      BIGSERIAL PRIMARY KEY,
    product_id              BIGINT NOT NULL REFERENCES products(id),
    inbound_item_id         BIGINT NOT NULL REFERENCES inbound_items(id),
    batch_number            VARCHAR(50) NOT NULL,
    arrival_date            DATE NOT NULL,
    expiry_date             DATE NOT NULL,
    quantity_received       INTEGER NOT NULL CHECK (quantity_received > 0),
    quantity_remaining      INTEGER NOT NULL CHECK (quantity_remaining >= 0),
    purchase_unit_price     DECIMAL(12, 2) NOT NULL CHECK (purchase_unit_price >= 0),
    expiry_status           VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
                            CHECK (expiry_status IN ('NORMAL', 'EXPIRING_SOON', 'EXPIRED')),
    version                 INTEGER NOT NULL DEFAULT 0,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT NOT NULL REFERENCES users(id),
    updated_by              BIGINT NOT NULL REFERENCES users(id),

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
```

Optimistic locking via `version` column (`@Version` in JPA) prevents overselling under concurrent orders.

#### `inventory_transactions`

```sql
CREATE TABLE inventory_transactions (
    id                      BIGSERIAL PRIMARY KEY,
    batch_id                BIGINT NOT NULL REFERENCES batches(id),
    product_id              BIGINT NOT NULL REFERENCES products(id),
    transaction_type        VARCHAR(20) NOT NULL
                            CHECK (transaction_type IN ('IN', 'OUT', 'ROLLBACK', 'DESTROY', 'ADJUST')),
    quantity_delta          INTEGER NOT NULL CHECK (quantity_delta != 0),
    quantity_before         INTEGER NOT NULL CHECK (quantity_before >= 0),
    quantity_after          INTEGER NOT NULL CHECK (quantity_after >= 0),
    related_document_type   VARCHAR(20),
    related_document_id     BIGINT,
    note                    TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT NOT NULL REFERENCES users(id),

    CHECK (quantity_after = quantity_before + quantity_delta)
);

CREATE INDEX idx_inv_tx_batch_time ON inventory_transactions(batch_id, created_at DESC);
CREATE INDEX idx_inv_tx_product_time ON inventory_transactions(product_id, created_at DESC);
CREATE INDEX idx_inv_tx_type_time ON inventory_transactions(transaction_type, created_at DESC);
CREATE INDEX idx_inv_tx_related ON inventory_transactions(related_document_type, related_document_id);
```

Append-only: rows are never updated or deleted. `related_document_type/id` is a polymorphic soft reference to `orders`, `inbound_records`, or `destructions`.

#### `orders`

```sql
CREATE TABLE orders (
    id                      BIGSERIAL PRIMARY KEY,
    order_number            VARCHAR(30) NOT NULL UNIQUE,
    customer_id             BIGINT NOT NULL REFERENCES customers(id),
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'ROLLED_BACK')),
    subtotal_amount         DECIMAL(12, 2) NOT NULL CHECK (subtotal_amount >= 0),
    gst_amount              DECIMAL(12, 2) NOT NULL CHECK (gst_amount >= 0),
    total_amount            DECIMAL(12, 2) NOT NULL CHECK (total_amount >= 0),
    rolled_back_at          TIMESTAMP WITH TIME ZONE,
    rolled_back_by          BIGINT REFERENCES users(id),
    rollback_reason         TEXT,
    remarks                 TEXT,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT NOT NULL REFERENCES users(id),
    updated_by              BIGINT NOT NULL REFERENCES users(id),

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
```

Format for `order_number`: `ORD-YYYYMMDD-NNN`.

#### `order_items`

```sql
CREATE TABLE order_items (
    id                          BIGSERIAL PRIMARY KEY,
    order_id                    BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id                  BIGINT NOT NULL REFERENCES products(id),
    batch_id                    BIGINT NOT NULL REFERENCES batches(id),
    product_name_snapshot       VARCHAR(200) NOT NULL,
    product_sku_snapshot        VARCHAR(50) NOT NULL,
    product_unit_snapshot       VARCHAR(20) NOT NULL,
    product_spec_snapshot       VARCHAR(100),
    quantity                    INTEGER NOT NULL CHECK (quantity > 0),
    unit_price                  DECIMAL(12, 2) NOT NULL CHECK (unit_price >= 0),
    subtotal                    DECIMAL(12, 2) NOT NULL CHECK (subtotal >= 0),
    is_gst_applicable           BOOLEAN NOT NULL,
    gst_amount                  DECIMAL(12, 2) NOT NULL CHECK (gst_amount >= 0),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CHECK (subtotal = quantity * unit_price)
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
CREATE INDEX idx_order_items_batch ON order_items(batch_id);
```

Snapshot columns preserve product state at order time, so historical invoices remain accurate after product edits.

#### `destructions`

```sql
CREATE TABLE destructions (
    id                              BIGSERIAL PRIMARY KEY,
    destruction_number              VARCHAR(30) NOT NULL UNIQUE,
    batch_id                        BIGINT NOT NULL REFERENCES batches(id),
    product_id                      BIGINT NOT NULL REFERENCES products(id),
    inventory_transaction_id        BIGINT NOT NULL REFERENCES inventory_transactions(id),
    quantity_destroyed              INTEGER NOT NULL CHECK (quantity_destroyed > 0),
    purchase_unit_price_snapshot    DECIMAL(12, 2) NOT NULL,
    loss_amount                     DECIMAL(12, 2) NOT NULL CHECK (loss_amount >= 0),
    reason                          TEXT NOT NULL,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      BIGINT NOT NULL REFERENCES users(id),

    CHECK (loss_amount = quantity_destroyed * purchase_unit_price_snapshot)
);

CREATE INDEX idx_destructions_batch ON destructions(batch_id);
CREATE INDEX idx_destructions_product_time ON destructions(product_id, created_at DESC);
CREATE INDEX idx_destructions_time ON destructions(created_at DESC);
```

Format for `destruction_number`: `DES-YYYYMMDD-NNN`. Retained permanently for audit and loss accounting.

#### `audit_logs`

```sql
CREATE TABLE audit_logs (
    id                          BIGSERIAL PRIMARY KEY,
    operator_id                 BIGINT REFERENCES users(id),
    operator_username_snapshot  VARCHAR(50),
    operator_role_snapshot      VARCHAR(20),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entity_type                 VARCHAR(50) NOT NULL,
    entity_id                   BIGINT,
    action                      VARCHAR(20) NOT NULL
                                CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'ROLLBACK', 'LOGIN', 'LOGIN_FAIL')),
    before_value                JSONB,
    after_value                 JSONB,
    ip_address                  VARCHAR(45),
    description                 TEXT
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_logs_operator_time ON audit_logs(operator_id, created_at DESC);
CREATE INDEX idx_audit_logs_action_time ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_logs_time ON audit_logs(created_at DESC);
```

Append-only. Written automatically via AOP `@Auditable` aspect. Records write operations only (no read-tracking). Retained permanently.

---

## 6. API Design

Base URL: `/api/v1`  
Auth: `Authorization: Bearer <access_token>` on all endpoints except auth  
Response envelope: `{ code, message, data }`  
Paginated responses: `data = { items[], total, page, size }`

---

### 6.1 Auth

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| POST | `/auth/login` | Login, returns access token + sets refresh cookie | Public |
| POST | `/auth/refresh` | Refresh access token using httpOnly cookie | Public |
| POST | `/auth/logout` | Invalidate refresh token | All |
| POST | `/auth/change-password` | Change own password | All |

---

### 6.2 Users

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/users` | List all users (paginated) | ADMIN |
| POST | `/users` | Create new user | ADMIN |
| GET | `/users/{id}` | Get user detail | ADMIN |
| PUT | `/users/{id}` | Update user (name, email, role) | ADMIN |
| PATCH | `/users/{id}/status` | Enable / disable user | ADMIN |
| POST | `/users/{id}/reset-password` | Reset another user's password | ADMIN |

---

### 6.3 Products

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/products` | List products, filter by category/name, paginated | All |
| POST | `/products` | Create product | ADMIN, WAREHOUSE |
| GET | `/products/{id}` | Get product detail + current stock | All |
| PUT | `/products/{id}` | Update product | ADMIN, WAREHOUSE |
| PATCH | `/products/{id}/status` | Deactivate / reactivate | ADMIN, WAREHOUSE |
| GET | `/products/categories` | List distinct categories (for dropdown) | All |
| GET | `/products/units` | List distinct units (for dropdown) | All |

---

### 6.4 Inbound

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/inbound` | List inbound records, filter by date/product | ADMIN, WAREHOUSE |
| POST | `/inbound` | Create inbound record + items + batches | ADMIN, WAREHOUSE |
| GET | `/inbound/{id}` | Get inbound record with all items | ADMIN, WAREHOUSE |
| PUT | `/inbound/{id}` | Update inbound record and items | ADMIN, WAREHOUSE |

---

### 6.5 Customers

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/customers` | List customers, filter by name/phone | ADMIN, MARKETING |
| POST | `/customers` | Create customer | ADMIN, MARKETING |
| GET | `/customers/{id}` | Get customer + order history | ADMIN, MARKETING |
| PUT | `/customers/{id}` | Update customer | ADMIN, MARKETING |
| PATCH | `/customers/{id}/status` | Deactivate / reactivate | ADMIN, MARKETING |

---

### 6.6 Orders

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/orders` | List orders, filter by customer/date/status | ADMIN, WAREHOUSE, MARKETING |
| POST | `/orders` | Create order (deducts stock via FIFO) | ADMIN, WAREHOUSE, MARKETING |
| GET | `/orders/{id}` | Get order detail + line items | ADMIN, WAREHOUSE, MARKETING |
| POST | `/orders/{id}/rollback` | Rollback order (soft delete, returns stock) | ADMIN, WAREHOUSE, MARKETING |
| GET | `/orders/{id}/invoice` | Get invoice data (for PDF render) | ADMIN, WAREHOUSE, MARKETING |

---

### 6.7 Inventory

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/inventory` | Current stock by product (aggregated) | All |
| GET | `/inventory/batches` | All batches, filter by product/expiry status | ADMIN, WAREHOUSE |
| GET | `/inventory/transactions` | Stock movement log, filter by product/type/date | All |
| GET | `/inventory/low-stock` | Products below safety stock threshold | All |
| GET | `/inventory/expiring` | Batches expiring within threshold days | All |

---

### 6.8 Destructions

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/destructions` | List destruction records, filter by date/product | ADMIN, WAREHOUSE, ACCOUNTANT |
| POST | `/destructions` | Record a batch destruction | ADMIN, WAREHOUSE |
| GET | `/destructions/{id}` | Get destruction detail | ADMIN, WAREHOUSE, ACCOUNTANT |

---

### 6.9 Reports (Accountant)

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/reports/sales/export` | Export sales records as Excel/CSV (`?from=&to=&format=`) | ADMIN, ACCOUNTANT |
| GET | `/reports/losses/export` | Export destruction loss report as Excel/CSV | ADMIN, ACCOUNTANT |

---

### 6.10 Audit Logs

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/audit-logs` | List audit logs, filter by user/entity/action/date | ADMIN |

---

### 6.11 Dashboard

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/dashboard/summary` | Sales summary, expiry warnings, low stock count | All |

---

### 6.12 System Settings

| Method | Path | Description | Roles |
|--------|------|-------------|-------|
| GET | `/settings` | Get system settings (expiry warning days, etc.) | ADMIN |
| PUT | `/settings` | Update system settings | ADMIN |

---

## 7. Key Business Flows

See flow diagrams in the design session. Summary:

**Flow 1 — Login + token refresh**
User submits credentials → BCrypt verify + lock check → issue Access Token (body, 2h) + Refresh Token (httpOnly cookie, 7d) → on expiry, POST /auth/refresh auto-sends cookie → new access token issued.

**Flow 2 — Create order (FIFO)**
Select customer + cart → stock check → FIFO batch deduction (ORDER BY expiry_date ASC) with optimistic lock (`@Version`) → persist orders + order_items → write inventory_transactions (OUT) → return invoice data.

**Flow 3 — Order rollback**
Operator triggers rollback → set status=ROLLED_BACK, record rolled_back_by/at → return stock to original batches → write inventory_transactions (ROLLBACK) → write audit_log.

**Flow 4 — Daily expiry scan**
@Scheduled fires at 00:00 → scan all batches where quantity_remaining > 0 → compare expiry_date vs today + threshold → update expiry_status (NORMAL / EXPIRING_SOON / EXPIRED) → Dashboard reads updated statuses on next login.

**Flow 5 — Batch destruction**
Operator submits form (quantity + reason) → deduct batch quantity → write inventory_transactions (DESTROY) → write destructions record with loss_amount snapshot.

---

## 8. Tech Stack & Rationale

| Layer | Choice | Why |
|-------|--------|-----|
| Backend language | Java 21 | Current project baseline; modern LTS with strong typing and good Spring Boot support |
| Backend framework | Spring Boot 3.5.x | Current repository version; mature ecosystem with strong auto-configuration |
| Security | Spring Security 6 + JWT | Native RBAC support; stateless JWT fits single-page app architecture |
| ORM | Spring Data JPA + Hibernate | Reduces SQL boilerplate; `@Version` gives optimistic locking for free |
| Database | PostgreSQL 15 | JSONB for audit log; `pg_trgm` for fuzzy search; partial indexes; better than MySQL for this use case |
| DB migration | Flyway | Schema changes are versioned and repeatable; no manual ALTER TABLE on production |
| Scheduled jobs | Spring `@Scheduled` | Zero dependency; sufficient for single-instance deployment |
| Audit logging | AOP + `@Auditable` | Cross-cutting concern stays out of business code |
| Excel export | Apache POI | Standard Java library for Excel generation |
| PDF generation | OpenPDF | Invoice PDF generation; open-source iText fork |
| Frontend framework | React 19 + TypeScript 6 | Matches current frontend scaffold while preserving type safety and modern React tooling |
| Frontend routing | React Router 7 | Matches current frontend dependency version and supports protected-route structure for auth |
| Build tool | Vite | Fast dev server; significantly faster than CRA |
| Server state | TanStack Query | Caching, background refresh, loading/error states out of the box |
| Client state | Zustand | Lightweight; avoids Redux boilerplate for small client state |
| UI components | Tailwind CSS + shadcn/ui | Utility-first CSS; shadcn gives accessible components without heavy dependency |
| Password hashing | BCrypt cost=12 | Deliberately slow; defeats brute-force; MD5/SHA are unsafe for passwords |
| Containerisation | Docker + Docker Compose | Reproducible environments; easy deployment to any cloud |
| CI/CD | GitHub Actions | Free for public repos; integrates natively with GitHub |
| Cloud | AWS EC2 (Sydney) | Most common in NZ job market; free tier covers this project |

---


## 9. Security Considerations

| Area | Measure |
|------|---------|
| Password storage | BCrypt hash (cost=12). Plaintext and MD5/SHA are prohibited. |
| Authentication | JWT. Access Token 2h in memory, Refresh Token 7d in httpOnly + Secure + SameSite=Strict cookie. |
| Authorisation | RBAC enforced at Spring Security filter level on every request. |
| Brute force | Account locked for 10 minutes after 5 consecutive failed logins. |
| First login | `must_change_password` flag forces password change before any other action. |
| Input validation | `@Valid` on all controller request DTOs. |
| SQL injection | JPA parameterised queries only. No string concatenation in queries. |
| Data integrity | Database-level CHECK constraints as last line of defence. |
| Audit trail | Every write operation logged with operator, role, timestamp, before/after values. |
| HTTPS | Nginx terminates SSL. HTTP redirects to HTTPS. Let's Encrypt certificate. |
| Secrets | No secrets in source code. All credentials via environment variables. |

---

## 10. Risks & Open Questions

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Concurrent orders oversell stock | Low (< 10 users) | High | Optimistic locking (`@Version`) on batches table |
| Expiry scan misses a day (app restart at midnight) | Low | Low | Scan is idempotent; next day's scan corrects it |
| AWS free tier expires after 12 months | Certain | Medium | Migrate to t3.micro paid (~$10/month) or move to other provider |
| PDF library licensing (iText) | Low | Medium | Using OpenPDF (open-source fork) — no commercial license needed |
| Invoice GST calculation rounding | Low | Medium | All amounts stored as DECIMAL(12,2); GST calculated per line item then summed |

---

## 11. Milestones & Timeline

Estimated 6 weeks for MVP, working solo with AI assistance.

| Milestone | Weeks | Deliverables |
|-----------|-------|--------------|
| M1 — Foundation | 1 | Project scaffold, Docker Compose, Flyway schema, JWT auth, RBAC, default admin account, auth API foundation |
| M2 — Master data | 2 | Frontend login page, auth flow integration, products CRUD, customers CRUD, user management, basic frontend pages |
| M3 — Inventory | 3 | Inbound records, batch management, inventory queries, stock transaction log |
| M4 — Sales | 4 | Shopping cart, FIFO order creation, optimistic lock, invoice PDF, order rollback |
| M5 — Operations | 5 | Expiry scan (@Scheduled), batch destruction, dashboard, financial export (Excel) |
| M6 — Engineering | 6 | Unit + integration tests, GitHub Actions CI/CD, AWS EC2 deploy, README |

Each milestone ends with a GitHub Release tag (v0.1.0-M1 through v0.6.0-M6).

---

## 12. References

- Requirements Document: `./requirements.md`
- Development Workflow Diagram: `./0-1完整流程.png`

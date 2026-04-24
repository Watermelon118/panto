# Panto — Requirements Specification

**Version**: 2.2  
**Last Updated**: 2026-04-24  

---

## 1. Business Background

Panto is a warehouse management system for small food wholesale businesses in New Zealand, based on a real single-warehouse B2B operation. Customers (restaurants, companies, commercial buyers) communicate orders via instant messaging, and internal staff enter orders into the system on behalf of customers. Payment happens outside the system via bank transfer.

## 2. System Goals

1. Manage food inbound stock (batches and expiry dates)
2. Manage B2B customer information
3. Allow staff to enter orders and generate NZ-compliant invoices
4. Automatically warn about expiring stock; support manual destruction with cost recording
5. Provide complete audit trail for all data modifications
6. Support financial export for accounting and auditing

## 3. Non-Goals

- Online payment integration
- Order status workflow (paid / shipped / delivered)
- Customer credit account management (maintained in Excel)
- Multi-warehouse, logistics, delivery routing
- Customer self-service ordering
- Mobile application
- User self-registration
- Supplier management

## 4. Roles & Permission Matrix

| Function                       | Manager | Warehouse | Marketing | Accountant |
|--------------------------------|---------|-----------|-----------|------------|
| User management                | ✓       |           |           |            |
| Product management             | ✓       | ✓         |           |            |
| Inbound (create / modify)      | ✓       | ✓         |           |            |
| Customer management            | ✓       |           | ✓         |            |
| Sales order / invoice          | ✓       | ✓         | ✓         |            |
| Rollback order                 | ✓       | ✓         | ✓         |            |
| Destroy expired stock          | ✓       | ✓         |           |            |
| View warnings / dashboard      | ✓       | ✓         | ✓         | ✓          |
| Financial export               | ✓       |           |           | ✓          |
| Audit log viewer               | ✓       |           |           |            |

## 5. Functional Modules

### 5.1 User & Permission Management
- Default admin account (`admin / admin`), force password change on first login
- No self-registration; manager creates accounts and assigns roles
- Manager can disable / enable accounts and reset passwords
- Password policy: minimum 8 characters, must include letters and digits
- Password storage: BCrypt (cost=12). MD5 / SHA family is prohibited for passwords
- Account lockout after 5 consecutive failed logins for 10 minutes
- Authentication: JWT (Access Token 2h + Refresh Token 7d)

### 5.2 Product Management
- Fields: SKU, name, category, specification, unit, reference purchase price, reference sale price, safety stock threshold, GST applicability
- Real-time stock view (aggregated across all batches)
- CRUD operations

### 5.3 Inbound Management
- Inbound record fields: product, quantity, batch number, arrival date, expiry date, actual purchase unit price, remarks
- Automatic stock and batch update
- Inbound records are editable (edits are captured in audit log)
- Query history by date or product

### 5.4 Customer Management
- Fields: company name, contact person, phone, address, remarks
- Customer detail view: order history, cumulative spend
- Soft delete to preserve historical orders

### 5.5 Sales Order
- Workflow: select customer → add items to cart → adjust unit price (optional, for discounts) → submit order
- Stock availability check with concurrency control (database-level to prevent overselling)
- Expired products can still be sold, but with a red warning
- Batch deduction strategy: FIFO (first in, first out, by expiry date)
- On success, show a two-page invoice: **Customer Copy** + **Office Copy**
- Invoice contents (NZ standard):
  - Invoice number and date
  - Seller: Panto company info, GST number
  - Buyer: customer company, address, contact, phone
  - Line items: name, specification, unit, unit price, quantity, subtotal
  - Subtotal (excl. GST) / GST 15% / Total (incl. GST)
  - Payment instructions (bank transfer)
- Downloadable as PDF, or printable directly from browser
- Auto-download is NOT triggered on order submission

### 5.6 Order Rollback
- Permission: manager, warehouse staff, marketing
- Soft delete: order status set to `ROLLED_BACK`, never physically deleted
- Stock is returned to the original batches it was deducted from
- Recorded in the audit log

### 5.7 Expiry Warning
- System setting: warning threshold in days (default 30, manager adjustable)
- Daily 00:00 scheduled scan of all batches (`@Scheduled`)
- Batch status: `NORMAL` / `EXPIRING_SOON` / `EXPIRED`
- All users see warnings in:
  - Dashboard warning card: "Expiring soon: N / Expired: N"
  - Top navigation badge
  - Login-time toast notification
- Detail view: product, batch, days remaining, quantity, action (destroy)

### 5.8 Stock Destruction
- Permission: manager, warehouse staff
- Applies to a specific batch; partial destruction is allowed
- Required fields: quantity to destroy, reason
- Deducts the batch; writes a `DESTROY` entry in the inventory transaction log
- Destruction records available as a dedicated list
- Included in accountant's exportable "loss report"

### 5.9 Inventory Management
- Query by product (aggregated) or by batch (arrival, expiry, quantity)
- Inventory Transaction Log (immutable):
  - Fields: product, batch, type (`IN` / `OUT` / `ROLLBACK` / `DESTROY` / `ADJUST`), delta, stock before / after, related document, operator, timestamp
- Products below safety stock are highlighted on dashboard

### 5.10 Financial Export (Accountant)
- Sales export by date range (Excel / CSV)
  - Includes: order ID, date, customer, line items, unit price, quantity, GST, total, operator
- Loss (destruction) export by date range (Excel / CSV)
  - Includes: destruction date, product, batch, quantity, purchase unit price, loss value, operator, reason

### 5.11 Audit Log
- All database writes are captured automatically via AOP
- Fields: operator, role, timestamp, object type, object ID, operation type, before / after snapshots
- Viewable by manager only; filterable by time, user, object

### 5.12 Dashboard
- Visible to all authenticated users; content varies by role:
  - **Manager / Marketing**: today + month sales, top 10 products, stock warnings, expiry warnings, pending tasks
  - **Warehouse**: stock warnings, expiry warnings, today's inbound / outbound, pending destruction
  - **Accountant**: month's sales total, month's loss total, quick export buttons

## 6. Non-Functional Requirements

### Runtime
- Web application, desktop browser first (Chrome, Edge, Safari)
- Single warehouse, single tenant

### Scale (for this project)
- Products: < 1,000 SKUs
- Customers: < 500
- Orders per month: < 500
- Inbound per month: < 100

### Performance
- Typical query response: < 1 second
- Concurrent users: < 10

### Data Retention
- All business data retained indefinitely (including rolled-back orders, destruction records, audit logs)

### Security
- HTTPS only
- BCrypt password hashing (cost=12)
- JWT authentication
- Role-based access control (RBAC) at API level
- Two-step confirmation for destructive operations (destroy, rollback, delete user)

### Maintainability
- Database schema versioned with Flyway
- Structured logging (SLF4J, JSON format)
- Configuration separate from code (application.yml + environment variables)
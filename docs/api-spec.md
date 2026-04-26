# Panto API Specification

## Overview

- Base URL: `/api/v1`
- Response envelope: `Result<T> = { code, message, data }`
- Access Token: returned in response body
- Refresh Token: returned in httpOnly cookie

## Common Response Codes

| Code | Meaning |
|------|---------|
| `SUCCESS` | Request succeeded |
| `VALIDATION_ERROR` | Request parameter validation failed |
| `AUTH_INVALID_CREDENTIALS` | Username or password is incorrect |
| `AUTH_ACCOUNT_LOCKED` | Account is temporarily locked |
| `AUTH_UNAUTHORIZED` | Not logged in or token is invalid |
| `AUTH_FORBIDDEN` | Current user is disabled or has no permission |
| `AUTH_PASSWORD_CHANGE_REQUIRED` | Current user must change password before continuing |
| `AUTH_CURRENT_PASSWORD_INCORRECT` | Current password is incorrect |
| `USER_NOT_FOUND` | User does not exist |
| `USER_USERNAME_ALREADY_EXISTS` | Username already taken |
| `CUSTOMER_NOT_FOUND` | Customer does not exist |
| `PRODUCT_NOT_FOUND` | Product does not exist |
| `PRODUCT_SKU_ALREADY_EXISTS` | Product SKU already exists |
| `INBOUND_NOT_FOUND` | Inbound record does not exist |
| `INBOUND_PRODUCT_NOT_FOUND` | A product referenced in the inbound items does not exist |
| `INBOUND_HAS_STOCK_MOVEMENT` | Inbound record cannot be modified because stock has already been consumed |
| `ORDER_CUSTOMER_NOT_FOUND` | Customer does not exist or is inactive |
| `ORDER_PRODUCT_NOT_FOUND` | A product referenced in the order does not exist |
| `ORDER_PRODUCT_INACTIVE` | A product referenced in the order is inactive |
| `ORDER_DUPLICATE_PRODUCT` | The order contains duplicate products |
| `ORDER_NOT_FOUND` | Order does not exist |
| `ORDER_ALREADY_ROLLED_BACK` | Order has already been rolled back |
| `ORDER_INSUFFICIENT_STOCK` | Available stock is insufficient for the requested quantity |
| `ORDER_STOCK_CONFLICT` | Stock changed concurrently; the order should be retried |
| `DESTRUCTION_NOT_FOUND` | Destruction record does not exist |
| `DESTRUCTION_BATCH_NOT_FOUND` | Target batch does not exist or cannot be destroyed |
| `DESTRUCTION_INSUFFICIENT_STOCK` | Batch remaining stock is insufficient for the requested destruction quantity |
| `DESTRUCTION_STOCK_CONFLICT` | Batch stock changed concurrently; the destruction should be retried |
| `INTERNAL_SERVER_ERROR` | Unexpected server error |

## Auth APIs

### POST `/auth/login`

User login with username and password.

#### Request Body

```json
{
  "username": "admin",
  "password": "admin"
}
```

#### Validation Rules

- `username`: required, cannot be blank
- `password`: required, cannot be blank

#### Success Response

HTTP Status: `200 OK`

Set-Cookie:

```text
refresh_token=<jwt>; HttpOnly; SameSite=Strict; Path=/api/v1/auth
```

Response body:

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userId": 1,
    "username": "admin",
    "role": "ADMIN",
    "mustChangePassword": true
  }
}
```

#### Failure Responses

Invalid username or password:

HTTP Status: `400 Bad Request`

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "用户名或密码错误",
  "data": null
}
```

Account disabled:

HTTP Status: `400 Bad Request`

```json
{
  "code": "AUTH_FORBIDDEN",
  "message": "账号已停用",
  "data": null
}
```

Account locked:

HTTP Status: `400 Bad Request`

```json
{
  "code": "AUTH_ACCOUNT_LOCKED",
  "message": "账号已被锁定，请10分钟后再试",
  "data": null
}
```

Validation failure:

HTTP Status: `400 Bad Request`

```json
{
  "code": "VALIDATION_ERROR",
  "message": "请求参数校验失败",
  "data": {
    "username": "用户名不能为空"
  }
}
```

#### Notes

- Failed login attempts are recorded in `login_attempts`
- Account is locked for 10 minutes after 5 consecutive failed logins
- Successful login updates `users.last_login_at`

### POST `/auth/refresh`

Refresh access token using refresh token cookie.

#### Request Body

No request body.

#### Cookie Requirement

Request must include:

```text
Cookie: refresh_token=<jwt>
```

#### Success Response

HTTP Status: `200 OK`

Set-Cookie:

```text
refresh_token=<new-jwt>; HttpOnly; SameSite=Strict; Path=/api/v1/auth
```

Response body:

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "accessToken": "<new-jwt>",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userId": 1,
    "username": "admin",
    "role": "ADMIN",
    "mustChangePassword": true
  }
}
```

#### Failure Responses

Missing or invalid refresh token:

HTTP Status: `400 Bad Request`

```json
{
  "code": "AUTH_UNAUTHORIZED",
  "message": "未登录或登录已失效",
  "data": null
}
```

Disabled user:

HTTP Status: `400 Bad Request`

```json
{
  "code": "AUTH_FORBIDDEN",
  "message": "账号已停用",
  "data": null
}
```

Locked user:

HTTP Status: `400 Bad Request`

```json
{
  "code": "AUTH_ACCOUNT_LOCKED",
  "message": "账号已被锁定，请10分钟后再试",
  "data": null
}
```

#### Notes

- Refresh token is rotated on every successful refresh
- Refresh flow re-checks current user status from database
- If user is deleted, disabled, or locked, refresh request is rejected

### POST `/auth/change-password`

Change the password for the current logged-in user. This endpoint also clears the first-login password-change flag and returns a fresh token pair.

#### Request Body

```json
{
  "currentPassword": "admin123",
  "newPassword": "newpass123"
}
```

#### Validation Rules

- `currentPassword`: required, cannot be blank
- `newPassword`: required, min length `8`, max length `100`, must contain at least one letter and one digit

#### Success Response

HTTP Status: `200 OK`

Set-Cookie:

```text
refresh_token=<new-jwt>; HttpOnly; SameSite=Strict; Path=/api/v1/auth
```

Response body:

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "accessToken": "<new-jwt>",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userId": 1,
    "username": "admin",
    "role": "ADMIN",
    "mustChangePassword": false
  }
}
```

#### Failure Responses

Current password is incorrect:

HTTP Status: `400 Bad Request`

```json
{
  "code": "AUTH_CURRENT_PASSWORD_INCORRECT",
  "message": "当前密码错误",
  "data": null
}
```

Authentication invalid:

HTTP Status: `400 Bad Request`

```json
{
  "code": "AUTH_UNAUTHORIZED",
  "message": "未登录或登录已失效",
  "data": null
}
```

#### Notes

- After a successful password change, `mustChangePassword` becomes `false`
- The backend returns a fresh access token and rotated refresh token immediately

### POST `/auth/logout`

Logout the current user by clearing the refresh token cookie on the client.

#### Request Body

No request body.

#### Success Response

HTTP Status: `200 OK`

Set-Cookie:

```text
refresh_token=; HttpOnly; SameSite=Strict; Path=/api/v1/auth; Max-Age=0
```

Response body:

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": null
}
```

## Audit Log APIs

> Audit log APIs require `ADMIN` role.

### GET `/audit-logs`

Get paginated audit logs with optional filters.

#### Query Parameters

- `operatorId`: optional, filters by operator user ID
- `entityType`: optional, exact match ignoring case, e.g. `ORDER`
- `action`: optional, one of `CREATE`, `UPDATE`, `DELETE`, `ROLLBACK`, `LOGIN`, `LOGIN_FAIL`
- `dateFrom`: optional, format `yyyy-MM-dd`, filters logs created on or after this date
- `dateTo`: optional, format `yyyy-MM-dd`, filters logs created before the next day of this date
- `page`: optional, default `0`
- `size`: optional, default `20`, max `100`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "items": [
      {
        "id": 900,
        "operatorId": 1,
        "operatorUsername": "admin",
        "operatorRole": "ADMIN",
        "entityType": "ORDER",
        "entityId": 500,
        "action": "ROLLBACK",
        "description": "回滚订单",
        "ipAddress": "127.0.0.1",
        "beforeValue": {
          "status": "ACTIVE"
        },
        "afterValue": {
          "status": "ROLLED_BACK"
        },
        "createdAt": "2026-04-26T10:15:30+12:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Notes

- Successful write operations store before/after snapshots automatically through AOP
- Login success and login failure events are also recorded in the same audit log stream
- Sensitive fields such as password hashes and token fields are removed from stored snapshots

#### Access Rules

- Roles: `ADMIN`

## Product APIs

### GET `/products`

Get paginated products with optional filters.

#### Query Parameters

- `keyword`: optional, filters by SKU or product name
- `category`: optional, exact category match
- `active`: optional, filters by active status
- `page`: optional, default `0`
- `size`: optional, default `20`, max `100`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "items": [
      {
        "id": 1,
        "sku": "SKU-001",
        "name": "Frozen Dumplings",
        "category": "Frozen Food",
        "unit": "carton",
        "referencePurchasePrice": 12.50,
        "referenceSalePrice": 18.80,
        "safetyStock": 20,
        "gstApplicable": true,
        "active": true,
        "currentStock": 36
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

### GET `/products/{id}`

Get product detail with current aggregated stock.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "id": 1,
    "sku": "SKU-001",
    "name": "Frozen Dumplings",
    "category": "Frozen Food",
    "specification": "1kg x 10",
    "unit": "carton",
    "referencePurchasePrice": 12.50,
    "referenceSalePrice": 18.80,
    "safetyStock": 20,
    "gstApplicable": true,
    "active": true,
    "currentStock": 36,
    "createdAt": "2026-04-25T08:00:00Z",
    "updatedAt": "2026-04-25T10:30:00Z",
    "createdBy": 1,
    "updatedBy": 7
  }
}
```

#### Failure Responses

Product not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "产品不存在",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

### POST `/products`

Create a product.

#### Request Body

```json
{
  "sku": "SKU-001",
  "name": "Frozen Dumplings",
  "category": "Frozen Food",
  "specification": "1kg x 10",
  "unit": "carton",
  "referencePurchasePrice": 12.50,
  "referenceSalePrice": 18.80,
  "safetyStock": 20,
  "gstApplicable": true
}
```

#### Validation Rules

- `sku`: required, max length `50`
- `name`: required, max length `200`
- `category`: required, max length `50`
- `specification`: optional, max length `100`
- `unit`: required, max length `20`
- `referencePurchasePrice`: required, `>= 0`, scale `2`
- `referenceSalePrice`: required, `>= 0`, scale `2`
- `safetyStock`: required, `>= 0`
- `gstApplicable`: required

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /products/{id}`.

#### Failure Responses

Duplicate SKU:

HTTP Status: `400 Bad Request`

```json
{
  "code": "PRODUCT_SKU_ALREADY_EXISTS",
  "message": "产品 SKU 已存在",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`

### PUT `/products/{id}`

Update a product.

#### Request Body

Same as `POST /products`.

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /products/{id}`.

#### Failure Responses

- `PRODUCT_NOT_FOUND`
- `PRODUCT_SKU_ALREADY_EXISTS`

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`

### PATCH `/products/{id}/status`

Enable or disable a product.

#### Request Body

```json
{
  "active": false
}
```

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /products/{id}`.

#### Failure Responses

- `PRODUCT_NOT_FOUND`

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`

### GET `/products/categories`

Get active product categories for dropdown options.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": [
    "Frozen Food",
    "Snacks"
  ]
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

### GET `/products/units`

Get active product units for dropdown options.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": [
    "box",
    "carton"
  ]
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

## Customer APIs

### GET `/customers`

Get paginated customers with optional filters.

#### Query Parameters

- `keyword`: optional, filters by company name or phone
- `active`: optional, filters by active status
- `page`: optional, default `0`
- `size`: optional, default `20`, max `100`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "items": [
      {
        "id": 1,
        "companyName": "Panto Trading Ltd",
        "contactPerson": "Alex Chen",
        "phone": "021888999",
        "email": "alex@panto.co.nz",
        "active": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`

### GET `/customers/{id}`

Get customer detail.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "id": 1,
    "companyName": "Panto Trading Ltd",
    "contactPerson": "Alex Chen",
    "phone": "021888999",
    "email": "alex@panto.co.nz",
    "address": "99 Queen Street",
    "gstNumber": "GST-7788",
    "remarks": "Preferred morning delivery",
    "active": true,
    "cumulativeSpend": 276.00,
    "totalOrderCount": 2,
    "orderHistory": [
      {
        "id": 500,
        "orderNumber": "ORD-20260425-001",
        "status": "ACTIVE",
        "totalAmount": 276.00,
        "createdAt": "2026-04-25T10:00:00Z"
      }
    ],
    "createdAt": "2026-04-25T08:00:00Z",
    "updatedAt": "2026-04-25T10:30:00Z",
    "createdBy": 1,
    "updatedBy": 7
  }
}
```

#### Failure Responses

Customer not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "CUSTOMER_NOT_FOUND",
  "message": "客户不存在",
  "data": null
}
```

#### Notes

- `cumulativeSpend` only sums `ACTIVE` orders
- `orderHistory` returns the most recent 20 orders for the customer
- `totalOrderCount` lets the frontend show whether additional historical orders exist beyond the current payload

#### Access Rules

- Roles: `ADMIN`, `MARKETING`

### POST `/customers`

Create a customer.

#### Request Body

```json
{
  "companyName": "Panto Trading Ltd",
  "contactPerson": "Alex Chen",
  "phone": "021888999",
  "email": "alex@panto.co.nz",
  "address": "99 Queen Street",
  "gstNumber": "GST-7788",
  "remarks": "Preferred morning delivery"
}
```

#### Validation Rules

- `companyName`: required, max length `200`
- `contactPerson`: optional, max length `100`
- `phone`: optional, max length `30`
- `email`: optional, must be a valid email, max length `100`
- `gstNumber`: optional, max length `20`

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /customers/{id}`.

#### Access Rules

- Roles: `ADMIN`, `MARKETING`

### PUT `/customers/{id}`

Update a customer.

#### Request Body

Same as `POST /customers`.

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /customers/{id}`.

#### Failure Responses

- `CUSTOMER_NOT_FOUND`

#### Access Rules

- Roles: `ADMIN`, `MARKETING`

### PATCH `/customers/{id}/status`

Enable or disable a customer.

#### Request Body

```json
{
  "active": false
}
```

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /customers/{id}`.

#### Failure Responses

- `CUSTOMER_NOT_FOUND`

#### Access Rules

- Roles: `ADMIN`, `MARKETING`

## Inventory APIs

> All Inventory APIs require authentication. All roles have read access.

### GET `/inventory`

Get paginated current stock summary by product (active products only).

#### Query Parameters

- `keyword`: optional, filters by SKU or product name
- `category`: optional, exact category match
- `page`: optional, default `0`
- `size`: optional, default `20`, max `100`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "items": [
      {
        "productId": 5,
        "sku": "APPLE001",
        "name": "Green Apple",
        "category": "Fruit",
        "unit": "carton",
        "safetyStock": 50,
        "currentStock": 30,
        "belowSafetyStock": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

### GET `/inventory/batches`

Get paginated batch list with optional filters.

#### Query Parameters

- `productId`: optional, filters by product
- `expiryStatus`: optional, one of `NORMAL`, `EXPIRING_SOON`, `EXPIRED`
- `page`: optional, default `0`
- `size`: optional, default `20`, max `100`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "items": [
      {
        "id": 10,
        "productId": 5,
        "productSku": "APPLE001",
        "productName": "Green Apple",
        "batchNumber": "APPLE001-20250425-001",
        "arrivalDate": "2025-04-25",
        "expiryDate": "2025-10-25",
        "quantityReceived": 100,
        "quantityRemaining": 80,
        "purchaseUnitPrice": 2.50,
        "expiryStatus": "NORMAL",
        "createdAt": "2025-04-25T08:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

### GET `/inventory/transactions`

Get paginated stock movement log, sorted by time descending.

#### Query Parameters

- `productId`: optional, filters by product
- `transactionType`: optional, one of `IN`, `OUT`, `ROLLBACK`, `DESTROY`, `ADJUST`
- `page`: optional, default `0`
- `size`: optional, default `20`, max `100`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "items": [
      {
        "id": 100,
        "batchId": 10,
        "batchNumber": "APPLE001-20250425-001",
        "productId": 5,
        "productSku": "APPLE001",
        "productName": "Green Apple",
        "transactionType": "IN",
        "quantityDelta": 100,
        "quantityBefore": 0,
        "quantityAfter": 100,
        "relatedDocumentType": "INBOUND",
        "relatedDocumentId": 1,
        "note": "Goods received, inbound record #1",
        "createdAt": "2025-04-25T08:00:00Z",
        "createdBy": 1
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Notes

- `quantityDelta` is positive for stock-in (`IN`, `ROLLBACK`, `ADJUST`+) and negative for stock-out (`OUT`, `DESTROY`, `ADJUST`-)
- Records are append-only and cannot be modified

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

### GET `/inventory/low-stock`

Get all active products whose current stock is below their safety stock threshold, sorted by current stock ascending.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": [
    {
      "productId": 5,
      "sku": "APPLE001",
      "name": "Green Apple",
      "category": "Fruit",
      "unit": "carton",
      "safetyStock": 50,
      "currentStock": 10,
      "belowSafetyStock": true
    }
  ]
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

### GET `/inventory/expiring`

Get all batches with remaining stock that expire within the specified number of days, sorted by expiry date ascending.

#### Query Parameters

- `withinDays`: optional, default `30`, min `1`, max `365`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": [
    {
      "id": 10,
      "productId": 5,
      "productSku": "APPLE001",
      "productName": "Green Apple",
      "batchNumber": "APPLE001-20250425-001",
      "arrivalDate": "2025-04-25",
      "expiryDate": "2025-05-10",
      "quantityReceived": 100,
      "quantityRemaining": 40,
      "purchaseUnitPrice": 2.50,
      "expiryStatus": "EXPIRING_SOON",
      "createdAt": "2025-04-25T08:00:00Z"
    }
  ]
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

## User APIs

> All User APIs require `ADMIN` role.

### GET `/users`

Get paginated user list.

#### Query Parameters

- `page`: optional, default `0`
- `size`: optional, default `20`, max `100`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "items": [
      {
        "id": 1,
        "username": "john.doe",
        "fullName": "John Doe",
        "email": "john@panto.co.nz",
        "role": "WAREHOUSE",
        "active": true,
        "mustChangePassword": false
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### GET `/users/{id}`

Get user detail.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "id": 1,
    "username": "john.doe",
    "fullName": "John Doe",
    "email": "john@panto.co.nz",
    "role": "WAREHOUSE",
    "active": true,
    "mustChangePassword": false,
    "lockedUntil": null,
    "lastLoginAt": "2026-04-25T09:00:00Z",
    "createdAt": "2026-04-25T08:00:00Z",
    "updatedAt": "2026-04-25T10:30:00Z",
    "createdBy": 1,
    "updatedBy": 1
  }
}
```

#### Failure Responses

User not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "USER_NOT_FOUND",
  "message": "用户不存在",
  "data": null
}
```

### POST `/users`

Create a user. The new user must change their password on first login.

#### Request Body

```json
{
  "username": "john.doe",
  "password": "Password1",
  "fullName": "John Doe",
  "email": "john@panto.co.nz",
  "role": "WAREHOUSE"
}
```

#### Validation Rules

- `username`: required, max length `50`
- `password`: required, min length `8`, max length `100`, must contain at least one letter and one digit
- `fullName`: required, max length `100`
- `email`: optional, must be a valid email, max length `100`
- `role`: required, one of `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /users/{id}`.

#### Failure Responses

Duplicate username:

HTTP Status: `400 Bad Request`

```json
{
  "code": "USER_USERNAME_ALREADY_EXISTS",
  "message": "用户名已存在",
  "data": null
}
```

### PUT `/users/{id}`

Update user profile. Username is not modifiable.

#### Request Body

```json
{
  "fullName": "John Smith",
  "email": "john.smith@panto.co.nz",
  "role": "ACCOUNTANT"
}
```

#### Validation Rules

- `fullName`: required, max length `100`
- `email`: optional, must be a valid email, max length `100`
- `role`: required, one of `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /users/{id}`.

#### Failure Responses

- `USER_NOT_FOUND`

### PATCH `/users/{id}/status`

Enable or disable a user. Disabling clears any active lock.

#### Request Body

```json
{
  "active": false
}
```

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /users/{id}`.

#### Failure Responses

- `USER_NOT_FOUND`

## Inbound APIs

### GET `/inbound`

Get paginated inbound records with optional filters.

#### Query Parameters

- `dateFrom`: optional, format `yyyy-MM-dd`, filters records on or after this date
- `dateTo`: optional, format `yyyy-MM-dd`, filters records on or before this date
- `productId`: optional, filters records that contain the specified product
- `page`: optional, default `0`
- `size`: optional, default `20`, max `100`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "items": [
      {
        "id": 1,
        "inboundNumber": "IN-20250425-001",
        "inboundDate": "2025-04-25",
        "itemCount": 3,
        "remarks": "April stock replenishment",
        "createdAt": "2025-04-25T08:00:00Z",
        "createdBy": 1
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

### GET `/inbound/{id}`

Get inbound record detail including all line items.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "id": 1,
    "inboundNumber": "IN-20250425-001",
    "inboundDate": "2025-04-25",
    "remarks": "April stock replenishment",
    "items": [
      {
        "id": 10,
        "productId": 5,
        "productSku": "APPLE001",
        "productName": "Green Apple",
        "batchNumber": "APPLE001-20250425-001",
        "expiryDate": "2025-10-25",
        "quantity": 100,
        "purchaseUnitPrice": 2.50,
        "remarks": null
      }
    ],
    "createdAt": "2025-04-25T08:00:00Z",
    "updatedAt": "2025-04-25T08:00:00Z",
    "createdBy": 1,
    "updatedBy": 1
  }
}
```

#### Failure Responses

Inbound record not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "INBOUND_NOT_FOUND",
  "message": "入库单不存在",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

### POST `/inbound`

Create an inbound record. Automatically generates:
- Inbound number in format `IN-YYYYMMDD-NNN`
- Batch number per item in format `{SKU}-YYYYMMDD-NNN`
- One `Batch` record per line item
- One `IN` inventory transaction per batch

#### Request Body

```json
{
  "inboundDate": "2025-04-25",
  "remarks": "April stock replenishment",
  "items": [
    {
      "productId": 5,
      "expiryDate": "2025-10-25",
      "quantity": 100,
      "purchaseUnitPrice": 2.50,
      "remarks": null
    }
  ]
}
```

#### Validation Rules

- `inboundDate`: required
- `remarks`: optional, max length `1000`
- `items`: required, min `1` item
- `items[].productId`: required, must reference an existing active product
- `items[].expiryDate`: required
- `items[].quantity`: required, `>= 1`
- `items[].purchaseUnitPrice`: required, `>= 0`, scale `2`
- `items[].remarks`: optional, max length `500`

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /inbound/{id}`.

#### Failure Responses

Product not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "INBOUND_PRODUCT_NOT_FOUND",
  "message": "入库明细中包含不存在的产品",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`

### PUT `/inbound/{id}`

Update an inbound record. Only allowed if no stock from any batch of this record has been consumed. On success, all existing items, batches, and `IN` transactions are deleted and recreated from the new request.

#### Request Body

Same as `POST /inbound`.

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /inbound/{id}`.

#### Failure Responses

Inbound record not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "INBOUND_NOT_FOUND",
  "message": "入库单不存在",
  "data": null
}
```

Stock already consumed:

HTTP Status: `400 Bad Request`

```json
{
  "code": "INBOUND_HAS_STOCK_MOVEMENT",
  "message": "该入库单已有库存被使用，无法修改",
  "data": null
}
```

#### Notes

- If any batch derived from this inbound has `quantity_remaining < quantity_received`, the update is rejected
- Batch numbers are regenerated from the new request data

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`

## Order APIs

### GET `/orders`

Get paginated orders with optional customer, date, and status filters.

#### Query Parameters

- `customerId`: optional, filters by customer
- `dateFrom`: optional, format `yyyy-MM-dd`, filters orders created on or after this date
- `dateTo`: optional, format `yyyy-MM-dd`, filters orders created on or before this date
- `status`: optional, one of `ACTIVE`, `ROLLED_BACK`
- `page`: optional, default `0`
- `size`: optional, default `20`, max `100`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "items": [
      {
        "id": 500,
        "orderNumber": "ORD-20260425-001",
        "customerId": 1,
        "customerCompanyName": "Fresh Dumplings Ltd",
        "status": "ACTIVE",
        "itemCount": 2,
        "subtotalAmount": 240.00,
        "gstAmount": 36.00,
        "totalAmount": 276.00,
        "createdAt": "2026-04-25T10:00:00Z",
        "createdBy": 1
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`

### GET `/orders/{id}`

Get full order detail including persisted batch-level order items.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "id": 500,
    "orderNumber": "ORD-20260425-001",
    "customerId": 1,
    "customerCompanyName": "Fresh Dumplings Ltd",
    "status": "ACTIVE",
    "subtotalAmount": 240.00,
    "gstAmount": 36.00,
    "totalAmount": 276.00,
    "remarks": "Deliver before noon",
    "items": [
      {
        "id": 1100,
        "productId": 5,
        "batchId": 100,
        "batchNumber": "DUMP001-20260420-001",
        "batchExpiryDate": "2026-05-01",
        "batchExpiryStatus": "EXPIRING_SOON",
        "productSku": "DUMP001",
        "productName": "Frozen Dumplings",
        "productUnit": "carton",
        "productSpecification": "1kg x 10",
        "quantity": 5,
        "unitPrice": 20.00,
        "subtotal": 100.00,
        "gstApplicable": true,
        "gstAmount": 15.00
      }
    ],
    "createdAt": "2026-04-25T10:00:00Z",
    "updatedAt": "2026-04-25T10:00:00Z",
    "createdBy": 1,
    "updatedBy": 1
  }
}
```

#### Failure Responses

Order not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "订单不存在",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`

### POST `/orders`

Create a sales order. Stock is deducted by FIFO order using batches sorted by expiry date ascending.

#### Request Body

```json
{
  "customerId": 1,
  "remarks": "Deliver before noon",
  "items": [
    {
      "productId": 5,
      "quantity": 12,
      "unitPrice": 20.00
    }
  ]
}
```

#### Validation Rules

- `customerId`: required, must reference an active customer
- `remarks`: optional, max length `1000`
- `items`: required, min `1` item
- `items[].productId`: required, must reference an existing active product
- `items[].quantity`: required, `>= 1`
- `items[].unitPrice`: optional, `>= 0`, scale `2`; defaults to the product reference sale price when omitted
- The same product cannot appear more than once in `items`

#### Business Rules

- Available inventory is checked before the order is persisted
- Batch deduction uses FIFO by `expiry_date ASC`
- If one batch is not enough, one request line may be split into multiple persisted `order_items`
- `order_items` store product snapshot fields so historical order data remains stable after product edits
- GST is calculated per persisted order item and then summed at order level
- Expiring or expired stock is still eligible for sale; warning display is handled by the frontend
- Batch optimistic locking failures are translated to `ORDER_STOCK_CONFLICT`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "id": 500,
    "orderNumber": "ORD-20260425-001",
    "customerId": 1,
    "customerCompanyName": "Fresh Dumplings Ltd",
    "status": "ACTIVE",
    "subtotalAmount": 240.00,
    "gstAmount": 36.00,
    "totalAmount": 276.00,
    "remarks": "Deliver before noon",
    "items": [
      {
        "id": 1100,
        "productId": 5,
        "batchId": 100,
        "batchNumber": "DUMP001-20260420-001",
        "batchExpiryDate": "2026-05-01",
        "batchExpiryStatus": "EXPIRING_SOON",
        "productSku": "DUMP001",
        "productName": "Frozen Dumplings",
        "productUnit": "carton",
        "productSpecification": "1kg x 10",
        "quantity": 5,
        "unitPrice": 20.00,
        "subtotal": 100.00,
        "gstApplicable": true,
        "gstAmount": 15.00
      },
      {
        "id": 1101,
        "productId": 5,
        "batchId": 101,
        "batchNumber": "DUMP001-20260421-001",
        "batchExpiryDate": "2026-05-07",
        "batchExpiryStatus": "NORMAL",
        "productSku": "DUMP001",
        "productName": "Frozen Dumplings",
        "productUnit": "carton",
        "productSpecification": "1kg x 10",
        "quantity": 7,
        "unitPrice": 20.00,
        "subtotal": 140.00,
        "gstApplicable": true,
        "gstAmount": 21.00
      }
    ],
    "createdAt": "2026-04-25T10:00:00Z",
    "updatedAt": "2026-04-25T10:00:00Z",
    "createdBy": 1,
    "updatedBy": 1
  }
}
```

#### Failure Responses

Customer not found or inactive:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_CUSTOMER_NOT_FOUND",
  "message": "客户不存在或已停用",
  "data": null
}
```

Product not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_PRODUCT_NOT_FOUND",
  "message": "订单中包含不存在的商品",
  "data": null
}
```

Product inactive:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_PRODUCT_INACTIVE",
  "message": "订单中包含已停用的商品",
  "data": null
}
```

Duplicate product lines:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_DUPLICATE_PRODUCT",
  "message": "订单中存在重复商品，请合并后再提交",
  "data": null
}
```

Insufficient stock:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_INSUFFICIENT_STOCK",
  "message": "库存不足，无法创建订单",
  "data": null
}
```

Concurrent stock conflict:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_STOCK_CONFLICT",
  "message": "库存已被其他操作更新，请重试",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`

### POST `/orders/{id}/rollback`

Rollback an order. The order status is changed to `ROLLED_BACK` and stock is returned to the original deducted batches.

#### Request Body

```json
{
  "reason": "Customer cancelled"
}
```

#### Validation Rules

- `reason`: required, cannot be blank, max length `1000`

#### Business Rules

- Only active orders can be rolled back
- Stock is restored to the exact original batches recorded in `order_items`
- One `ROLLBACK` inventory transaction is written per restored order item
- The original order row is retained and updated to `ROLLED_BACK`
- Batch optimistic locking failures are translated to `ORDER_STOCK_CONFLICT`

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /orders/{id}`, except `status` becomes `ROLLED_BACK`.

#### Failure Responses

Order not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "订单不存在",
  "data": null
}
```

Order already rolled back:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_ALREADY_ROLLED_BACK",
  "message": "订单已回滚，不能重复操作",
  "data": null
}
```

Concurrent stock conflict:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_STOCK_CONFLICT",
  "message": "库存已被其他操作更新，请重试",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`

### GET `/orders/{id}/invoice`

Get invoice-ready order data for print or PDF rendering.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "orderId": 500,
    "invoiceNumber": "ORD-20260425-001",
    "invoiceDate": "2026-04-25T10:00:00Z",
    "status": "ACTIVE",
    "customer": {
      "companyName": "Fresh Dumplings Ltd",
      "contactPerson": "Alex Chen",
      "phone": "021888999",
      "address": "99 Queen Street",
      "gstNumber": "GST-7788"
    },
    "items": [
      {
        "productSku": "DUMP001",
        "productName": "Frozen Dumplings",
        "productSpecification": "1kg x 10",
        "productUnit": "carton",
        "quantity": 12,
        "unitPrice": 20.00,
        "subtotal": 240.00,
        "gstApplicable": true,
        "gstAmount": 36.00
      }
    ],
    "subtotalAmount": 240.00,
    "gstAmount": 36.00,
    "totalAmount": 276.00,
    "remarks": "Deliver before noon",
    "paymentInstructions": "Bank transfer"
  }
}
```

#### Notes

- Invoice lines are aggregated by product snapshot + unit price, even if the actual stock deduction used multiple batches
- `invoiceNumber` currently reuses `orderNumber`

#### Failure Responses

Order not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "订单不存在",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`

### POST `/users/{id}/reset-password`

Reset a user's password. Sets `mustChangePassword = true` and clears any active lock.

#### Request Body

```json
{
  "newPassword": "NewPass1"
}
```

#### Validation Rules

- `newPassword`: required, min length `8`, max length `100`, must contain at least one letter and one digit

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /users/{id}`.

#### Failure Responses

- `USER_NOT_FOUND`

## Destruction APIs

### GET `/destructions`

Get paginated destruction records with optional product and date filters.

#### Query Parameters

- `productId`: optional, filters by product
- `dateFrom`: optional, format `yyyy-MM-dd`, filters records created on or after this date
- `dateTo`: optional, format `yyyy-MM-dd`, filters records created on or before this date
- `page`: optional, default `0`
- `size`: optional, default `20`, max `100`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "items": [
      {
        "id": 500,
        "destructionNumber": "DES-20260426-001",
        "batchId": 100,
        "batchNumber": "DUMP001-20260410-001",
        "batchExpiryDate": "2026-04-28",
        "batchExpiryStatus": "EXPIRING_SOON",
        "productId": 5,
        "productSku": "DUMP001",
        "productName": "Frozen Dumplings",
        "quantityDestroyed": 4,
        "purchaseUnitPriceSnapshot": 12.50,
        "lossAmount": 50.00,
        "reason": "Expired stock",
        "createdAt": "2026-04-26T09:30:00Z",
        "createdBy": 1
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `ACCOUNTANT`

### GET `/destructions/{id}`

Get a destruction record detail including associated batch information.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "id": 500,
    "destructionNumber": "DES-20260426-001",
    "batchId": 100,
    "batchNumber": "DUMP001-20260410-001",
    "batchExpiryDate": "2026-04-28",
    "batchExpiryStatus": "EXPIRING_SOON",
    "batchQuantityRemaining": 8,
    "productId": 5,
    "productSku": "DUMP001",
    "productName": "Frozen Dumplings",
    "inventoryTransactionId": 900,
    "quantityDestroyed": 4,
    "purchaseUnitPriceSnapshot": 12.50,
    "lossAmount": 50.00,
    "reason": "Expired stock",
    "createdAt": "2026-04-26T09:30:00Z",
    "createdBy": 1
  }
}
```

#### Failure Responses

Destruction record not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "DESTRUCTION_NOT_FOUND",
  "message": "销毁记录不存在",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `ACCOUNTANT`

### POST `/destructions`

Create a destruction record for a specific batch and deduct stock immediately.

#### Request Body

```json
{
  "batchId": 100,
  "quantityDestroyed": 4,
  "reason": "Expired stock"
}
```

#### Validation Rules

- `batchId`: required
- `quantityDestroyed`: required, `>= 1`
- `reason`: required, cannot be blank, max length `1000`

#### Business Rules

- Destruction is always applied to a specific batch
- Partial destruction is allowed as long as `quantityDestroyed <= quantityRemaining`
- The service writes one `DESTROY` inventory transaction per destruction record
- `purchaseUnitPriceSnapshot` and `lossAmount` are stored on the destruction row for later accounting/export use
- Batch optimistic locking failures are translated to `DESTRUCTION_STOCK_CONFLICT`

#### Success Response

HTTP Status: `200 OK`

Response body structure is the same as `GET /destructions/{id}`.

#### Failure Responses

Batch not found:

HTTP Status: `400 Bad Request`

```json
{
  "code": "DESTRUCTION_BATCH_NOT_FOUND",
  "message": "批次不存在或已无法销毁",
  "data": null
}
```

Insufficient remaining stock:

HTTP Status: `400 Bad Request`

```json
{
  "code": "DESTRUCTION_INSUFFICIENT_STOCK",
  "message": "批次剩余库存不足，无法销毁",
  "data": null
}
```

Concurrent stock conflict:

HTTP Status: `400 Bad Request`

```json
{
  "code": "DESTRUCTION_STOCK_CONFLICT",
  "message": "批次库存已被其他操作更新，请重试",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`

## Reports APIs

File export endpoints return raw file downloads instead of the standard `Result<T>` envelope. This keeps frontend blob downloads straightforward for CSV/XLSX exports.

### GET `/reports/sales/export`

Export active sales records within a date range as CSV or Excel.

#### Query Parameters

- `from`: required, format `yyyy-MM-dd`, inclusive start date
- `to`: required, format `yyyy-MM-dd`, inclusive end date
- `format`: optional, `csv` or `xlsx`, default `xlsx`

#### Response

HTTP Status: `200 OK`

Content types:

- `text/csv; charset=UTF-8`
- `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

File naming pattern:

- `sales-report-<from>-to-<to>.csv`
- `sales-report-<from>-to-<to>.xlsx`

#### Export Columns

- `orderNumber`
- `orderCreatedAt`
- `customerCompanyName`
- `productSku`
- `productName`
- `quantity`
- `unitPrice`
- `subtotal`
- `gstAmount`
- `lineTotal`
- `operatorId`

#### Business Rules

- Only `ACTIVE` orders are exported
- One row is produced per persisted `order_item`
- `lineTotal = subtotal + gstAmount`
- Empty result sets still return a file with header row(s)

#### Failure Responses

Validation failure:

HTTP Status: `400 Bad Request`

```json
{
  "code": "VALIDATION_ERROR",
  "message": "from must be on or before to",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `ACCOUNTANT`

### GET `/reports/losses/export`

Export destruction loss records within a date range as CSV or Excel.

#### Query Parameters

- `from`: required, format `yyyy-MM-dd`, inclusive start date
- `to`: required, format `yyyy-MM-dd`, inclusive end date
- `format`: optional, `csv` or `xlsx`, default `xlsx`

#### Response

HTTP Status: `200 OK`

Content types:

- `text/csv; charset=UTF-8`
- `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

File naming pattern:

- `loss-report-<from>-to-<to>.csv`
- `loss-report-<from>-to-<to>.xlsx`

#### Export Columns

- `destructionNumber`
- `destructionCreatedAt`
- `productSku`
- `productName`
- `batchNumber`
- `quantityDestroyed`
- `purchaseUnitPrice`
- `lossAmount`
- `operatorId`
- `reason`

#### Business Rules

- One row is produced per destruction record
- `lossAmount` uses the persisted snapshot value on the destruction row
- Empty result sets still return a file with header row(s)

#### Failure Responses

Validation failure:

HTTP Status: `400 Bad Request`

```json
{
  "code": "VALIDATION_ERROR",
  "message": "from and to are required",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`, `ACCOUNTANT`

## Dashboard APIs

### GET `/dashboard/summary`

Return role-aware dashboard summary data for the current authenticated user.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "role": "ADMIN",
    "warnings": {
      "lowStockCount": 2,
      "expiringSoonCount": 3,
      "expiredCount": 1
    },
    "managerSummary": {
      "todaySalesTotal": 1280.50,
      "monthSalesTotal": 5666.80,
      "pendingTaskCount": 6,
      "topProducts": [
        {
          "productId": 5,
          "productSku": "DUMP001",
          "productName": "Frozen Dumplings",
          "quantitySold": 24,
          "salesAmount": 552.00
        }
      ]
    },
    "warehouseSummary": null,
    "accountantSummary": null
  }
}
```

#### Role-specific Fields

- `warnings`: always returned for all roles
- `managerSummary`: returned for `ADMIN` and `MARKETING`
- `warehouseSummary`: returned for `WAREHOUSE`
- `accountantSummary`: returned for `ACCOUNTANT`

#### Field Semantics

- `warnings.lowStockCount`: number of active products below safety stock
- `warnings.expiringSoonCount`: number of active batches with `EXPIRING_SOON`
- `warnings.expiredCount`: number of active batches with `EXPIRED`
- `managerSummary.pendingTaskCount`: `lowStockCount + expiringSoonCount + expiredCount`
- `managerSummary.topProducts`: current-month top 10 sold products ranked by quantity, then sales amount
- `warehouseSummary.todayInboundCount`: number of inbound records dated today
- `warehouseSummary.todayOutboundCount`: number of active orders created today
- `warehouseSummary.pendingDestructionCount`: number of expired batches still holding stock
- `accountantSummary.monthSalesTotal`: current-month active order total
- `accountantSummary.monthLossTotal`: current-month destruction loss total

#### Access Rules

- Roles: `ADMIN`, `WAREHOUSE`, `MARKETING`, `ACCOUNTANT`

## Settings APIs

System-wide configuration values, e.g. expiry-warning threshold consumed by the daily expiry scan and dashboard widgets. Only the manager role may read or modify settings.

The endpoint returns a typed object so the frontend does not need to parse string values. Future settings can be added as additional fields on the same DTO.

### GET `/api/v1/settings`

Return current system settings.

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "expiryWarningDays": 30
  }
}
```

#### Access Rules

- Roles: `ADMIN`

### PUT `/api/v1/settings`

Update system settings. Returns the updated settings on success.

#### Request Body

```json
{
  "expiryWarningDays": 45
}
```

#### Validation Rules

- `expiryWarningDays`: required, integer, range `[0, 3650]`

#### Success Response

HTTP Status: `200 OK`

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {
    "expiryWarningDays": 45
  }
}
```

#### Failure Responses

Validation failure:

HTTP Status: `400 Bad Request`

```json
{
  "code": "VALIDATION_ERROR",
  "message": "expiry_warning_days must be between 0 and 3650",
  "data": null
}
```

#### Access Rules

- Roles: `ADMIN`

#### Notes

- Changes take effect immediately for new requests; the next scheduled expiry scan (00:00 Pacific/Auckland) uses the latest threshold
- The service caches values in memory and refreshes the cache on every successful update

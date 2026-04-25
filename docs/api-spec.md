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
| `USER_NOT_FOUND` | User does not exist |
| `USER_USERNAME_ALREADY_EXISTS` | Username already taken |
| `CUSTOMER_NOT_FOUND` | Customer does not exist |
| `PRODUCT_NOT_FOUND` | Product does not exist |
| `PRODUCT_SKU_ALREADY_EXISTS` | Product SKU already exists |
| `INBOUND_NOT_FOUND` | Inbound record does not exist |
| `INBOUND_PRODUCT_NOT_FOUND` | A product referenced in the inbound items does not exist |
| `INBOUND_HAS_STOCK_MOVEMENT` | Inbound record cannot be modified because stock has already been consumed |
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

- Roles: `ADMIN`, `MARKETING`

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

- Milestone 2 currently returns customer base information only
- Order history will be added when the order module is implemented

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

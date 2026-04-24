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
| `PRODUCT_NOT_FOUND` | Product does not exist |
| `PRODUCT_SKU_ALREADY_EXISTS` | Product SKU already exists |
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

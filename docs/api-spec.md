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

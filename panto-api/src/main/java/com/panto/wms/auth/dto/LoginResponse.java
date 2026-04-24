package com.panto.wms.auth.dto;

import com.panto.wms.auth.domain.UserRole;

/**
 * 登录成功响应数据。
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    Long userId,
    String username,
    UserRole role,
    boolean mustChangePassword
) {
}

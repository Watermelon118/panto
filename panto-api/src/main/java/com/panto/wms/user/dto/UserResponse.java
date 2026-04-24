package com.panto.wms.user.dto;

import com.panto.wms.auth.domain.UserRole;
import java.time.OffsetDateTime;

/**
 * 用户详情响应体。
 *
 * @param id 用户 ID
 * @param username 用户名
 * @param fullName 姓名
 * @param email 邮箱
 * @param role 用户角色
 * @param active 用户是否启用
 * @param mustChangePassword 是否必须修改密码
 * @param lockedUntil 锁定截止时间
 * @param lastLoginAt 最后登录时间
 * @param createdAt 创建时间
 * @param updatedAt 最后更新时间
 * @param createdBy 创建人 ID
 * @param updatedBy 最后更新人 ID
 */
public record UserResponse(
    Long id,
    String username,
    String fullName,
    String email,
    UserRole role,
    Boolean active,
    Boolean mustChangePassword,
    OffsetDateTime lockedUntil,
    OffsetDateTime lastLoginAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long createdBy,
    Long updatedBy
) {
}

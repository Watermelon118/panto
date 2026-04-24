package com.panto.wms.user.dto;

import com.panto.wms.auth.domain.UserRole;

/**
 * 用户列表摘要响应体。
 *
 * @param id 用户 ID
 * @param username 用户名
 * @param fullName 姓名
 * @param email 邮箱
 * @param role 用户角色
 * @param active 用户是否启用
 * @param mustChangePassword 是否必须修改密码
 */
public record UserSummaryResponse(
    Long id,
    String username,
    String fullName,
    String email,
    UserRole role,
    Boolean active,
    Boolean mustChangePassword
) {
}

package com.panto.wms.user.dto;

import com.panto.wms.auth.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新用户请求体。
 *
 * @param fullName 姓名
 * @param email 邮箱，可为空
 * @param role 用户角色
 */
public record UpdateUserRequest(
    @NotBlank @Size(max = 100) String fullName,
    @Email @Size(max = 100) String email,
    @NotNull UserRole role
) {
}

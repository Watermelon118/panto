package com.panto.wms.user.dto;

import com.panto.wms.auth.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建用户请求体。
 *
 * @param username 用户名
 * @param password 初始密码
 * @param fullName 姓名
 * @param email 邮箱，可为空
 * @param role 用户角色
 */
public record CreateUserRequest(
    @NotBlank @Size(max = 50) String username,
    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "密码必须至少 8 位，且同时包含字母和数字"
    )
    String password,
    @NotBlank @Size(max = 100) String fullName,
    @Email @Size(max = 100) String email,
    @NotNull UserRole role
) {
}

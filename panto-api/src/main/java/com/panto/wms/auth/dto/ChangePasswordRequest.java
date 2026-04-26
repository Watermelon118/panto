package com.panto.wms.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改本人密码请求体。
 *
 * @param currentPassword 当前密码
 * @param newPassword 新密码
 */
public record ChangePasswordRequest(
    @NotBlank(message = "当前密码不能为空")
    String currentPassword,

    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "密码必须至少 8 位，且同时包含字母和数字"
    )
    String newPassword
) {
}

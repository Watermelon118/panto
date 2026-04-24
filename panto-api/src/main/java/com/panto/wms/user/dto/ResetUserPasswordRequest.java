package com.panto.wms.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 重置用户密码请求体。
 *
 * @param newPassword 新密码
 */
public record ResetUserPasswordRequest(
    @NotBlank
    @Size(min = 8, max = 100)
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "密码必须至少 8 位，且同时包含字母和数字"
    )
    String newPassword
) {
}

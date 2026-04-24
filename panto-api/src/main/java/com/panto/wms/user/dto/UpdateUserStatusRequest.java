package com.panto.wms.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 更新用户启用状态请求体。
 *
 * @param active 用户是否启用
 */
public record UpdateUserStatusRequest(@NotNull Boolean active) {
}

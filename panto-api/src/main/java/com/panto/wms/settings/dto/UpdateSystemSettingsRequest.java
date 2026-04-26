package com.panto.wms.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 系统设置更新请求体。
 *
 * @param expiryWarningDays 过期预警天数，0-3650
 */
public record UpdateSystemSettingsRequest(
    @NotNull
    @Min(0)
    @Max(3650)
    Integer expiryWarningDays
) {
}

package com.panto.wms.settings.dto;

/**
 * 系统设置响应体。后续新增设置项可以在此追加字段。
 *
 * @param expiryWarningDays 过期预警天数
 */
public record SystemSettingsResponse(int expiryWarningDays) {
}

package com.panto.wms.customer.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 更新客户启用状态请求体。
 *
 * @param active 客户是否启用
 */
public record UpdateCustomerStatusRequest(@NotNull Boolean active) {
}

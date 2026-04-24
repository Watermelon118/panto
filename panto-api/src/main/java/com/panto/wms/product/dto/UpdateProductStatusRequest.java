package com.panto.wms.product.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 更新产品启用状态请求体。
 *
 * @param active 产品是否启用
 */
public record UpdateProductStatusRequest(@NotNull Boolean active) {
}

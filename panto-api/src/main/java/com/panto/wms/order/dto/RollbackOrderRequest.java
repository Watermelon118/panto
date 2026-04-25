package com.panto.wms.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 订单回滚请求体。
 *
 * @param reason 回滚原因
 */
public record RollbackOrderRequest(
    @NotBlank @Size(max = 1000) String reason
) {
}

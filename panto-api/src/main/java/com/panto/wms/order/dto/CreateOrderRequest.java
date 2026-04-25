package com.panto.wms.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建销售订单请求体。
 *
 * @param customerId 客户 ID
 * @param remarks 备注，可为空
 * @param items 购物车商品列表，至少一项
 */
public record CreateOrderRequest(
    @NotNull Long customerId,
    @Size(max = 1000) String remarks,
    @NotNull @Size(min = 1) List<@Valid CreateOrderItemRequest> items
) {
}

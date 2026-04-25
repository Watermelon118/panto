package com.panto.wms.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建订单时的单个购物车商品请求体。
 *
 * @param productId 商品 ID
 * @param quantity 购买数量
 * @param unitPrice 成交单价；为空时回退到商品参考售价
 */
public record CreateOrderItemRequest(
    @NotNull Long productId,
    @NotNull @Min(1) Integer quantity,
    @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice
) {
}

package com.panto.wms.inbound.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 入库明细行请求体，供创建和更新入库单复用。
 *
 * @param productId        产品 ID
 * @param expiryDate       到期日
 * @param quantity         数量
 * @param purchaseUnitPrice 采购单价
 * @param remarks          备注，可为空
 */
public record InboundItemRequest(
    @NotNull Long productId,
    @NotNull LocalDate expiryDate,
    @NotNull @Min(1) Integer quantity,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal purchaseUnitPrice,
    @Size(max = 500) String remarks
) {
}

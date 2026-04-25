package com.panto.wms.inbound.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 入库明细行响应体。
 *
 * @param id                明细 ID
 * @param productId         产品 ID
 * @param productSku        产品 SKU
 * @param productName       产品名称
 * @param batchNumber       批次号
 * @param expiryDate        到期日
 * @param quantity          数量
 * @param purchaseUnitPrice 采购单价
 * @param remarks           备注，可为空
 */
public record InboundItemResponse(
    Long id,
    Long productId,
    String productSku,
    String productName,
    String batchNumber,
    LocalDate expiryDate,
    Integer quantity,
    BigDecimal purchaseUnitPrice,
    String remarks
) {
}

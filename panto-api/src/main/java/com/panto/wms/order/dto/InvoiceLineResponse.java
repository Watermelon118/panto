package com.panto.wms.order.dto;

import java.math.BigDecimal;

/**
 * 发票明细行响应体。
 *
 * @param productSku 商品 SKU
 * @param productName 商品名称
 * @param productSpecification 商品规格
 * @param productUnit 商品单位
 * @param quantity 数量
 * @param unitPrice 成交单价
 * @param subtotal 小计
 * @param gstApplicable 是否适用 GST
 * @param gstAmount GST 金额
 */
public record InvoiceLineResponse(
    String productSku,
    String productName,
    String productSpecification,
    String productUnit,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal,
    Boolean gstApplicable,
    BigDecimal gstAmount
) {
}

package com.panto.wms.order.dto;

import com.panto.wms.inventory.domain.ExpiryStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单明细响应体。
 *
 * @param id 订单明细 ID
 * @param productId 商品 ID
 * @param batchId 批次 ID
 * @param batchNumber 实际扣减的批次号
 * @param batchExpiryDate 批次到期日
 * @param batchExpiryStatus 批次到期状态
 * @param productSku 商品 SKU 快照
 * @param productName 商品名称快照
 * @param productUnit 商品单位快照
 * @param productSpecification 商品规格快照
 * @param quantity 当前明细扣减数量
 * @param unitPrice 当前明细成交单价
 * @param subtotal 当前明细小计
 * @param gstApplicable 是否适用 GST
 * @param gstAmount 当前明细 GST 金额
 */
public record OrderItemResponse(
    Long id,
    Long productId,
    Long batchId,
    String batchNumber,
    LocalDate batchExpiryDate,
    ExpiryStatus batchExpiryStatus,
    String productSku,
    String productName,
    String productUnit,
    String productSpecification,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal subtotal,
    Boolean gstApplicable,
    BigDecimal gstAmount
) {
}

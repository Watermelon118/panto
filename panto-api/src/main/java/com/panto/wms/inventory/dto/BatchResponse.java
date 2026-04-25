package com.panto.wms.inventory.dto;

import com.panto.wms.inventory.domain.ExpiryStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 库存批次响应体。
 *
 * @param id                批次 ID
 * @param productId         产品 ID
 * @param productSku        产品 SKU
 * @param productName       产品名称
 * @param batchNumber       批次号
 * @param arrivalDate       到货日期
 * @param expiryDate        到期日
 * @param quantityReceived  入库数量
 * @param quantityRemaining 当前剩余数量
 * @param purchaseUnitPrice 采购单价
 * @param expiryStatus      到期状态
 * @param createdAt         创建时间
 */
public record BatchResponse(
    Long id,
    Long productId,
    String productSku,
    String productName,
    String batchNumber,
    LocalDate arrivalDate,
    LocalDate expiryDate,
    Integer quantityReceived,
    Integer quantityRemaining,
    BigDecimal purchaseUnitPrice,
    ExpiryStatus expiryStatus,
    OffsetDateTime createdAt
) {
}

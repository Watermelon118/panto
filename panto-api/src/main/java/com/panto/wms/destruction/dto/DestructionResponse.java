package com.panto.wms.destruction.dto;

import com.panto.wms.inventory.domain.ExpiryStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 销毁记录详情响应体。
 *
 * @param id 销毁记录 ID
 * @param destructionNumber 销毁单号
 * @param batchId 批次 ID
 * @param batchNumber 批次号
 * @param batchExpiryDate 批次到期日
 * @param batchExpiryStatus 批次到期状态
 * @param batchQuantityRemaining 当前批次剩余库存
 * @param productId 产品 ID
 * @param productSku 产品 SKU
 * @param productName 产品名称
 * @param inventoryTransactionId 关联库存事务 ID
 * @param quantityDestroyed 销毁数量
 * @param purchaseUnitPriceSnapshot 销毁时采购单价快照
 * @param lossAmount 损耗金额
 * @param reason 销毁原因
 * @param createdAt 创建时间
 * @param createdBy 创建人 ID
 */
public record DestructionResponse(
    Long id,
    String destructionNumber,
    Long batchId,
    String batchNumber,
    LocalDate batchExpiryDate,
    ExpiryStatus batchExpiryStatus,
    Integer batchQuantityRemaining,
    Long productId,
    String productSku,
    String productName,
    Long inventoryTransactionId,
    Integer quantityDestroyed,
    BigDecimal purchaseUnitPriceSnapshot,
    BigDecimal lossAmount,
    String reason,
    OffsetDateTime createdAt,
    Long createdBy
) {
}

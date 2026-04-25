package com.panto.wms.inventory.dto;

import com.panto.wms.inventory.domain.TransactionType;
import java.time.OffsetDateTime;

/**
 * 库存事务响应体。
 *
 * @param id                  事务 ID
 * @param batchId             批次 ID
 * @param batchNumber         批次号
 * @param productId           产品 ID
 * @param productSku          产品 SKU
 * @param productName         产品名称
 * @param transactionType     事务类型
 * @param quantityDelta       数量变化（正为入库，负为出库）
 * @param quantityBefore      变化前剩余数量
 * @param quantityAfter       变化后剩余数量
 * @param relatedDocumentType 关联单据类型，可为空
 * @param relatedDocumentId   关联单据 ID，可为空
 * @param note                备注，可为空
 * @param createdAt           事务创建时间
 * @param createdBy           操作人 ID
 */
public record InventoryTransactionResponse(
    Long id,
    Long batchId,
    String batchNumber,
    Long productId,
    String productSku,
    String productName,
    TransactionType transactionType,
    Integer quantityDelta,
    Integer quantityBefore,
    Integer quantityAfter,
    String relatedDocumentType,
    Long relatedDocumentId,
    String note,
    OffsetDateTime createdAt,
    Long createdBy
) {
}

package com.panto.wms.inventory.dto;

/**
 * 产品库存汇总响应体。
 *
 * @param productId      产品 ID
 * @param sku            产品 SKU
 * @param name           产品名称
 * @param category       产品分类
 * @param unit           库存单位
 * @param safetyStock    安全库存阈值
 * @param currentStock   当前可用库存（所有批次 quantity_remaining 之和）
 * @param belowSafetyStock 是否低于安全库存
 */
public record StockSummaryResponse(
    Long productId,
    String sku,
    String name,
    String category,
    String unit,
    Integer safetyStock,
    long currentStock,
    boolean belowSafetyStock
) {
}

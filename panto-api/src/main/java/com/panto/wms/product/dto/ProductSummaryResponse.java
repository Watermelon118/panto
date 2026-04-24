package com.panto.wms.product.dto;

import java.math.BigDecimal;

/**
 * 产品列表摘要响应体。
 *
 * @param id 产品 ID
 * @param sku 产品 SKU
 * @param name 产品名称
 * @param category 产品分类
 * @param unit 库存单位
 * @param referencePurchasePrice 参考采购价
 * @param referenceSalePrice 参考销售价
 * @param safetyStock 安全库存阈值
 * @param gstApplicable 是否适用 GST
 * @param active 产品是否启用
 * @param currentStock 当前聚合库存
 */
public record ProductSummaryResponse(
    Long id,
    String sku,
    String name,
    String category,
    String unit,
    BigDecimal referencePurchasePrice,
    BigDecimal referenceSalePrice,
    Integer safetyStock,
    Boolean gstApplicable,
    Boolean active,
    long currentStock
) {
}

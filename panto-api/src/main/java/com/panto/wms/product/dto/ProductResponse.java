package com.panto.wms.product.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 产品详情响应体。
 *
 * @param id 产品 ID
 * @param sku 产品 SKU
 * @param name 产品名称
 * @param category 产品分类
 * @param specification 产品规格，可为空
 * @param unit 库存单位
 * @param referencePurchasePrice 参考采购价
 * @param referenceSalePrice 参考销售价
 * @param safetyStock 安全库存阈值
 * @param gstApplicable 是否适用 GST
 * @param active 产品是否启用
 * @param currentStock 当前聚合库存
 * @param createdAt 创建时间
 * @param updatedAt 最后更新时间
 * @param createdBy 创建人 ID
 * @param updatedBy 最后更新人 ID
 */
public record ProductResponse(
    Long id,
    String sku,
    String name,
    String category,
    String specification,
    String unit,
    BigDecimal referencePurchasePrice,
    BigDecimal referenceSalePrice,
    Integer safetyStock,
    Boolean gstApplicable,
    Boolean active,
    long currentStock,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long createdBy,
    Long updatedBy
) {
}

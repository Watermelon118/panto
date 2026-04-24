package com.panto.wms.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 更新产品请求体。
 *
 * @param sku 产品 SKU
 * @param name 产品名称
 * @param category 产品分类
 * @param specification 产品规格，可为空
 * @param unit 库存单位
 * @param referencePurchasePrice 参考采购价
 * @param referenceSalePrice 参考销售价
 * @param safetyStock 安全库存阈值
 * @param gstApplicable 是否适用 GST
 */
public record UpdateProductRequest(
    @NotBlank @Size(max = 50) String sku,
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 50) String category,
    @Size(max = 100) String specification,
    @NotBlank @Size(max = 20) String unit,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal referencePurchasePrice,
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal referenceSalePrice,
    @NotNull @Min(0) Integer safetyStock,
    @NotNull Boolean gstApplicable
) {
}

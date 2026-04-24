package com.panto.wms.product.repository;

/**
 * 产品聚合库存投影。
 */
public interface ProductStockView {

    /**
     * 返回产品 ID。
     *
     * @return 产品 ID
     */
    Long getProductId();

    /**
     * 返回当前聚合库存。
     *
     * @return 当前聚合库存
     */
    Long getCurrentStock();
}

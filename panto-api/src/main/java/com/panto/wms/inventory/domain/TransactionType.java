package com.panto.wms.inventory.domain;

/**
 * 库存事务类型。
 */
public enum TransactionType {
    IN,
    OUT,
    ROLLBACK,
    DESTROY,
    ADJUST
}

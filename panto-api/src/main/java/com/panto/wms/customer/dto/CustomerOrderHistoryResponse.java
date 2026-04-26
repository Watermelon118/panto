package com.panto.wms.customer.dto;

import com.panto.wms.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 客户详情中的订单历史项响应体。
 *
 * @param id 订单 ID
 * @param orderNumber 订单号
 * @param status 订单状态
 * @param totalAmount 订单总金额
 * @param createdAt 订单创建时间
 */
public record CustomerOrderHistoryResponse(
    Long id,
    String orderNumber,
    OrderStatus status,
    BigDecimal totalAmount,
    OffsetDateTime createdAt
) {
}

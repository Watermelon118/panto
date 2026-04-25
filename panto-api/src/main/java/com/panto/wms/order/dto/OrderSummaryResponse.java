package com.panto.wms.order.dto;

import com.panto.wms.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 订单列表摘要响应体。
 *
 * @param id 订单 ID
 * @param orderNumber 订单号
 * @param customerId 客户 ID
 * @param customerCompanyName 客户公司名称
 * @param status 订单状态
 * @param itemCount 订单明细行数
 * @param subtotalAmount 未税金额
 * @param gstAmount GST 金额
 * @param totalAmount 含税总金额
 * @param createdAt 创建时间
 * @param createdBy 创建人 ID
 */
public record OrderSummaryResponse(
    Long id,
    String orderNumber,
    Long customerId,
    String customerCompanyName,
    OrderStatus status,
    int itemCount,
    BigDecimal subtotalAmount,
    BigDecimal gstAmount,
    BigDecimal totalAmount,
    OffsetDateTime createdAt,
    Long createdBy
) {
}

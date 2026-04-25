package com.panto.wms.order.dto;

import com.panto.wms.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 销售订单详情响应体。
 *
 * @param id 订单 ID
 * @param orderNumber 订单号
 * @param customerId 客户 ID
 * @param customerCompanyName 客户公司名称
 * @param status 订单状态
 * @param subtotalAmount 未税金额
 * @param gstAmount GST 金额
 * @param totalAmount 含税总金额
 * @param remarks 备注，可为空
 * @param items 订单明细列表
 * @param createdAt 创建时间
 * @param updatedAt 最后更新时间
 * @param createdBy 创建人 ID
 * @param updatedBy 最后更新人 ID
 */
public record OrderResponse(
    Long id,
    String orderNumber,
    Long customerId,
    String customerCompanyName,
    OrderStatus status,
    BigDecimal subtotalAmount,
    BigDecimal gstAmount,
    BigDecimal totalAmount,
    String remarks,
    List<OrderItemResponse> items,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long createdBy,
    Long updatedBy
) {
}

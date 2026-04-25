package com.panto.wms.order.dto;

import com.panto.wms.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 发票查询响应体。
 *
 * @param orderId 订单 ID
 * @param invoiceNumber 发票号
 * @param invoiceDate 开票时间
 * @param status 订单状态
 * @param customer 客户信息
 * @param items 发票明细行
 * @param subtotalAmount 未税金额
 * @param gstAmount GST 金额
 * @param totalAmount 含税总金额
 * @param remarks 备注
 * @param paymentInstructions 付款说明
 */
public record InvoiceResponse(
    Long orderId,
    String invoiceNumber,
    OffsetDateTime invoiceDate,
    OrderStatus status,
    InvoiceCustomerResponse customer,
    List<InvoiceLineResponse> items,
    BigDecimal subtotalAmount,
    BigDecimal gstAmount,
    BigDecimal totalAmount,
    String remarks,
    String paymentInstructions
) {
}

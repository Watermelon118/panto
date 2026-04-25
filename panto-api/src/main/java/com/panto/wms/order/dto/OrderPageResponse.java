package com.panto.wms.order.dto;

import java.util.List;

/**
 * 订单分页响应体。
 *
 * @param items 当前页数据
 * @param page 当前页码
 * @param size 每页条数
 * @param totalElements 总记录数
 * @param totalPages 总页数
 */
public record OrderPageResponse(
    List<OrderSummaryResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}

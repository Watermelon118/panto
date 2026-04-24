package com.panto.wms.customer.dto;

import java.util.List;

/**
 * 分页客户列表响应体。
 *
 * @param items 当前页数据
 * @param page 当前页码
 * @param size 每页条数
 * @param totalElements 总记录数
 * @param totalPages 总页数
 */
public record CustomerPageResponse(
    List<CustomerSummaryResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}

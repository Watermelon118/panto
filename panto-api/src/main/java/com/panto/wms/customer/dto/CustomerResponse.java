package com.panto.wms.customer.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 客户详情响应体。
 *
 * @param id 客户 ID
 * @param companyName 公司名称
 * @param contactPerson 联系人
 * @param phone 联系电话
 * @param email 邮箱
 * @param address 地址
 * @param gstNumber GST 编号
 * @param remarks 备注
 * @param active 客户是否启用
 * @param cumulativeSpend 客户累计消费，仅统计有效订单
 * @param totalOrderCount 客户历史订单总数
 * @param orderHistory 最近订单历史
 * @param createdAt 创建时间
 * @param updatedAt 最后更新时间
 * @param createdBy 创建人 ID
 * @param updatedBy 最后更新人 ID
 */
public record CustomerResponse(
    Long id,
    String companyName,
    String contactPerson,
    String phone,
    String email,
    String address,
    String gstNumber,
    String remarks,
    Boolean active,
    BigDecimal cumulativeSpend,
    long totalOrderCount,
    List<CustomerOrderHistoryResponse> orderHistory,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long createdBy,
    Long updatedBy
) {
}

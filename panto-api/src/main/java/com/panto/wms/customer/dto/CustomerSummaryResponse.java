package com.panto.wms.customer.dto;

/**
 * 客户列表摘要响应体。
 *
 * @param id 客户 ID
 * @param companyName 公司名称
 * @param contactPerson 联系人
 * @param phone 联系电话
 * @param email 邮箱
 * @param active 客户是否启用
 */
public record CustomerSummaryResponse(
    Long id,
    String companyName,
    String contactPerson,
    String phone,
    String email,
    Boolean active
) {
}

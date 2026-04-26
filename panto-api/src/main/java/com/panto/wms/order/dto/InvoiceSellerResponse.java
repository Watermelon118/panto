package com.panto.wms.order.dto;

/**
 * 发票卖方信息响应体。
 *
 * @param companyName 卖方公司名称
 * @param gstNumber 卖方 GST 编号
 * @param address 卖方地址
 * @param phone 卖方联系电话
 * @param email 卖方联系邮箱
 */
public record InvoiceSellerResponse(
    String companyName,
    String gstNumber,
    String address,
    String phone,
    String email
) {
}

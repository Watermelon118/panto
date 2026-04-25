package com.panto.wms.order.dto;

/**
 * 发票中的客户信息响应体。
 *
 * @param companyName 公司名称
 * @param contactPerson 联系人
 * @param phone 联系电话
 * @param address 地址
 * @param gstNumber GST 号码
 */
public record InvoiceCustomerResponse(
    String companyName,
    String contactPerson,
    String phone,
    String address,
    String gstNumber
) {
}

package com.panto.wms.settings.dto;

/**
 * 系统设置响应体。后续新增设置项可以在此追加字段。
 *
 * @param expiryWarningDays 过期预警天数
 * @param invoiceSellerCompanyName 发票卖方公司名称
 * @param invoiceSellerGstNumber 发票卖方 GST 编号
 * @param invoiceSellerAddress 发票卖方地址
 * @param invoiceSellerPhone 发票卖方联系电话
 * @param invoiceSellerEmail 发票卖方联系邮箱
 * @param invoicePaymentInstructions 发票付款说明
 */
public record SystemSettingsResponse(
    int expiryWarningDays,
    String invoiceSellerCompanyName,
    String invoiceSellerGstNumber,
    String invoiceSellerAddress,
    String invoiceSellerPhone,
    String invoiceSellerEmail,
    String invoicePaymentInstructions
) {
}

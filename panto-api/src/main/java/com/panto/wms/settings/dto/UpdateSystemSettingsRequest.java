package com.panto.wms.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 系统设置更新请求体。
 *
 * @param expiryWarningDays 过期预警天数，0-3650
 * @param invoiceSellerCompanyName 发票卖方公司名称
 * @param invoiceSellerGstNumber 发票卖方 GST 编号
 * @param invoiceSellerAddress 发票卖方地址
 * @param invoiceSellerPhone 发票卖方联系电话
 * @param invoiceSellerEmail 发票卖方联系邮箱
 * @param invoicePaymentInstructions 发票付款说明
 */
public record UpdateSystemSettingsRequest(
    @NotNull
    @Min(0)
    @Max(3650)
    Integer expiryWarningDays,
    @NotNull
    String invoiceSellerCompanyName,
    @NotNull
    String invoiceSellerGstNumber,
    @NotNull
    String invoiceSellerAddress,
    @NotNull
    String invoiceSellerPhone,
    @NotNull
    String invoiceSellerEmail,
    @NotNull
    String invoicePaymentInstructions
) {
}

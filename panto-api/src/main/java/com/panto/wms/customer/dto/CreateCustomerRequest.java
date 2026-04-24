package com.panto.wms.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建客户请求体。
 *
 * @param companyName 公司名称
 * @param contactPerson 联系人，可为空
 * @param phone 联系电话，可为空
 * @param email 邮箱，可为空
 * @param address 地址，可为空
 * @param gstNumber GST 编号，可为空
 * @param remarks 备注，可为空
 */
public record CreateCustomerRequest(
    @NotBlank @Size(max = 200) String companyName,
    @Size(max = 100) String contactPerson,
    @Size(max = 30) String phone,
    @Email @Size(max = 100) String email,
    String address,
    @Size(max = 20) String gstNumber,
    String remarks
) {
}

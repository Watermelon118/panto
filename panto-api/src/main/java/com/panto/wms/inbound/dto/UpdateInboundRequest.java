package com.panto.wms.inbound.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 更新入库单请求体。
 *
 * @param inboundDate 入库日期
 * @param remarks     备注，可为空
 * @param items       明细行列表，至少一行
 */
public record UpdateInboundRequest(
    @NotNull LocalDate inboundDate,
    @Size(max = 1000) String remarks,
    @NotNull @Size(min = 1) List<@Valid InboundItemRequest> items
) {
}

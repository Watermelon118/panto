package com.panto.wms.inbound.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 入库单列表摘要响应体。
 *
 * @param id            入库单 ID
 * @param inboundNumber 入库单号
 * @param inboundDate   入库日期
 * @param itemCount     明细行数
 * @param remarks       备注，可为空
 * @param createdAt     创建时间
 * @param createdBy     创建人 ID
 */
public record InboundSummaryResponse(
    Long id,
    String inboundNumber,
    LocalDate inboundDate,
    int itemCount,
    String remarks,
    OffsetDateTime createdAt,
    Long createdBy
) {
}

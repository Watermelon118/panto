package com.panto.wms.inbound.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 入库单详情响应体，含完整明细行。
 *
 * @param id            入库单 ID
 * @param inboundNumber 入库单号
 * @param inboundDate   入库日期
 * @param remarks       备注，可为空
 * @param items         明细行列表
 * @param createdAt     创建时间
 * @param updatedAt     最后更新时间
 * @param createdBy     创建人 ID
 * @param updatedBy     最后更新人 ID
 */
public record InboundDetailResponse(
    Long id,
    String inboundNumber,
    LocalDate inboundDate,
    String remarks,
    List<InboundItemResponse> items,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Long createdBy,
    Long updatedBy
) {
}

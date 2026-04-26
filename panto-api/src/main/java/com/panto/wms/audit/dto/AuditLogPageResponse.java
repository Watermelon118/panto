package com.panto.wms.audit.dto;

import java.util.List;

/**
 * 审计日志分页响应体。
 *
 * @param items 当前页数据
 * @param page 当前页码
 * @param size 每页条数
 * @param totalElements 总条数
 * @param totalPages 总页数
 */
public record AuditLogPageResponse(
    List<AuditLogResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}

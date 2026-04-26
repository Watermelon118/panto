package com.panto.wms.audit.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.panto.wms.audit.domain.AuditAction;
import java.time.OffsetDateTime;

/**
 * 审计日志详情响应体。
 *
 * @param id 审计日志 ID
 * @param operatorId 操作人 ID
 * @param operatorUsername 操作人用户名快照
 * @param operatorRole 操作人角色快照
 * @param entityType 对象类型
 * @param entityId 对象 ID
 * @param action 操作类型
 * @param description 操作描述
 * @param ipAddress IP 地址
 * @param beforeValue 操作前快照
 * @param afterValue 操作后快照
 * @param createdAt 创建时间
 */
public record AuditLogResponse(
    Long id,
    Long operatorId,
    String operatorUsername,
    String operatorRole,
    String entityType,
    Long entityId,
    AuditAction action,
    String description,
    String ipAddress,
    JsonNode beforeValue,
    JsonNode afterValue,
    OffsetDateTime createdAt
) {
}

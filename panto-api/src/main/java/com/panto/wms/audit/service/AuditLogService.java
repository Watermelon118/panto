package com.panto.wms.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.panto.wms.audit.domain.AuditAction;
import com.panto.wms.audit.dto.AuditLogPageResponse;
import com.panto.wms.audit.dto.AuditLogResponse;
import com.panto.wms.audit.entity.AuditLog;
import com.panto.wms.audit.repository.AuditLogRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 审计日志查询服务。
 */
@Service
public class AuditLogService {

    private static final OffsetDateTime DEFAULT_CREATED_FROM = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final OffsetDateTime DEFAULT_CREATED_TO = OffsetDateTime.parse("9999-12-31T23:59:59.999999999Z");

    private final AuditLogRepository auditLogRepository;

    /**
     * 创建审计日志查询服务。
     *
     * @param auditLogRepository 审计日志仓储
     */
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 写入一条审计日志。
     *
     * @param operatorId 操作人 ID，可为空
     * @param operatorUsername 操作人用户名快照，可为空
     * @param operatorRole 操作人角色快照，可为空
     * @param entityType 业务对象类型
     * @param entityId 业务对象 ID，可为空
     * @param action 审计动作
     * @param description 审计描述，可为空
     * @param ipAddress 客户端 IP，可为空
     * @param beforeValue 操作前快照，可为空
     * @param afterValue 操作后快照，可为空
     */
    @Transactional
    public void recordAuditLog(
        Long operatorId,
        String operatorUsername,
        String operatorRole,
        String entityType,
        Long entityId,
        AuditAction action,
        String description,
        String ipAddress,
        JsonNode beforeValue,
        JsonNode afterValue
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setCreatedAt(OffsetDateTime.now());
        auditLog.setOperatorId(operatorId);
        auditLog.setOperatorUsernameSnapshot(operatorUsername);
        auditLog.setOperatorRoleSnapshot(operatorRole);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setDescription(description);
        auditLog.setIpAddress(ipAddress);
        auditLog.setBeforeValue(beforeValue);
        auditLog.setAfterValue(afterValue);
        auditLogRepository.save(auditLog);
    }

    /**
     * 按条件分页查询审计日志。
     *
     * @param operatorId 操作人 ID，可为空
     * @param entityType 对象类型，可为空
     * @param action 操作类型，可为空
     * @param dateFrom 起始日期，可为空
     * @param dateTo 结束日期，可为空
     * @param page 页码
     * @param size 每页条数
     * @return 分页查询结果
     */
    @Transactional(readOnly = true)
    public AuditLogPageResponse listAuditLogs(
        Long operatorId,
        String entityType,
        AuditAction action,
        LocalDate dateFrom,
        LocalDate dateTo,
        int page,
        int size
    ) {
        String normalizedEntityType = normalize(entityType);
        String entityTypeUpper = normalizedEntityType == null
            ? null
            : normalizedEntityType.toUpperCase(Locale.ROOT);
        OffsetDateTime createdFrom = resolveCreatedFrom(dateFrom);
        OffsetDateTime createdTo = resolveCreatedTo(dateTo);
        Page<AuditLog> result = auditLogRepository.search(
            operatorId,
            entityTypeUpper,
            action,
            createdFrom,
            createdTo,
            PageRequest.of(page, size)
        );

        List<AuditLogResponse> items = result.getContent().stream()
            .map(this::toResponse)
            .toList();

        return new AuditLogPageResponse(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getOperatorId(),
            log.getOperatorUsernameSnapshot(),
            log.getOperatorRoleSnapshot(),
            log.getEntityType(),
            log.getEntityId(),
            log.getAction(),
            log.getDescription(),
            log.getIpAddress(),
            log.getBeforeValue(),
            log.getAfterValue(),
            log.getCreatedAt()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private OffsetDateTime resolveCreatedFrom(LocalDate date) {
        return date == null ? DEFAULT_CREATED_FROM : date.atStartOfDay(systemZone()).toOffsetDateTime();
    }

    private OffsetDateTime resolveCreatedTo(LocalDate date) {
        return date == null ? DEFAULT_CREATED_TO : date.plusDays(1).atStartOfDay(systemZone()).toOffsetDateTime();
    }

    private ZoneId systemZone() {
        return ZoneId.systemDefault();
    }
}

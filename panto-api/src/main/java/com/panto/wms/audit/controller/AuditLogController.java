package com.panto.wms.audit.controller;

import com.panto.wms.audit.domain.AuditAction;
import com.panto.wms.audit.dto.AuditLogPageResponse;
import com.panto.wms.audit.service.AuditLogService;
import com.panto.wms.common.api.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志相关 REST 接口控制器。
 */
@Validated
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 创建审计日志控制器。
     *
     * @param auditLogService 审计日志查询服务
     */
    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * 分页查询审计日志。
     *
     * @param operatorId 操作人 ID，可为空
     * @param entityType 对象类型，可为空
     * @param action 操作类型，可为空
     * @param dateFrom 起始日期，可为空
     * @param dateTo 结束日期，可为空
     * @param page 页码
     * @param size 每页条数
     * @return 分页审计日志
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AuditLogPageResponse> listAuditLogs(
        @RequestParam(required = false) Long operatorId,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) AuditAction action,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return Result.success(
            auditLogService.listAuditLogs(operatorId, entityType, action, dateFrom, dateTo, page, size)
        );
    }
}

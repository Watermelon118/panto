package com.panto.wms.audit.domain;

/**
 * 审计日志中的操作类型。
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    ROLLBACK,
    LOGIN,
    LOGIN_FAIL
}

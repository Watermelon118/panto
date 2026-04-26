package com.panto.wms.audit.repository;

import com.panto.wms.audit.domain.AuditAction;
import com.panto.wms.audit.entity.AuditLog;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 审计日志数据访问接口。
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 按筛选条件分页查询审计日志。
     *
     * @param operatorId 操作人 ID，可为空
     * @param entityType 对象类型，可为空
     * @param action 操作类型，可为空
     * @param createdFrom 起始时间，可为空
     * @param createdTo 结束时间，可为空
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("""
        select log
        from AuditLog log
        where (:operatorId is null or log.operatorId = :operatorId)
          and (:entityType is null or upper(log.entityType) = upper(:entityType))
          and (:action is null or log.action = :action)
          and (:createdFrom is null or log.createdAt >= :createdFrom)
          and (:createdTo is null or log.createdAt < :createdTo)
        order by log.createdAt desc, log.id desc
        """)
    Page<AuditLog> search(
        @Param("operatorId") Long operatorId,
        @Param("entityType") String entityType,
        @Param("action") AuditAction action,
        @Param("createdFrom") OffsetDateTime createdFrom,
        @Param("createdTo") OffsetDateTime createdTo,
        Pageable pageable
    );
}

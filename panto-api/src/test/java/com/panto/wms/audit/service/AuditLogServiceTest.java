package com.panto.wms.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.panto.wms.audit.domain.AuditAction;
import com.panto.wms.audit.entity.AuditLog;
import com.panto.wms.audit.repository.AuditLogRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * 审计日志服务测试。
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    @Captor
    private ArgumentCaptor<OffsetDateTime> createdFromCaptor;

    @Captor
    private ArgumentCaptor<OffsetDateTime> createdToCaptor;

    @Test
    void recordAuditLogShouldPersistSnapshotsAndMetadata() {
        ObjectNode beforeValue = OBJECT_MAPPER.createObjectNode().put("status", "ACTIVE");
        ObjectNode afterValue = OBJECT_MAPPER.createObjectNode().put("status", "ROLLED_BACK");

        auditLogService.recordAuditLog(
            7L,
            "alice",
            "ADMIN",
            "ORDER",
            99L,
            AuditAction.ROLLBACK,
            "回滚订单",
            "10.0.0.5",
            beforeValue,
            afterValue
        );

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog savedLog = auditLogCaptor.getValue();
        assertEquals(7L, savedLog.getOperatorId());
        assertEquals("alice", savedLog.getOperatorUsernameSnapshot());
        assertEquals("ADMIN", savedLog.getOperatorRoleSnapshot());
        assertEquals("ORDER", savedLog.getEntityType());
        assertEquals(99L, savedLog.getEntityId());
        assertEquals(AuditAction.ROLLBACK, savedLog.getAction());
        assertEquals("回滚订单", savedLog.getDescription());
        assertEquals("10.0.0.5", savedLog.getIpAddress());
        assertEquals(beforeValue, savedLog.getBeforeValue());
        assertEquals(afterValue, savedLog.getAfterValue());
        assertNotNull(savedLog.getCreatedAt());
    }

    @Test
    void listAuditLogsShouldNormalizeFiltersAndMapPage() {
        AuditLog auditLog = new AuditLog();
        auditLog.setId(5L);
        auditLog.setOperatorId(9L);
        auditLog.setOperatorUsernameSnapshot("john");
        auditLog.setOperatorRoleSnapshot("WAREHOUSE");
        auditLog.setEntityType("ORDER");
        auditLog.setEntityId(100L);
        auditLog.setAction(AuditAction.UPDATE);
        auditLog.setDescription("更新订单");
        auditLog.setIpAddress("127.0.0.1");
        auditLog.setBeforeValue(OBJECT_MAPPER.createObjectNode().put("status", "ACTIVE"));
        auditLog.setAfterValue(OBJECT_MAPPER.createObjectNode().put("status", "ROLLED_BACK"));
        auditLog.setCreatedAt(OffsetDateTime.parse("2026-04-26T10:15:30+12:00"));

        when(auditLogRepository.search(
            eq(9L),
            eq("ORDER"),
            eq(AuditAction.UPDATE),
            any(OffsetDateTime.class),
            any(OffsetDateTime.class),
            eq(PageRequest.of(1, 20))
        )).thenReturn(new PageImpl<>(List.of(auditLog), PageRequest.of(1, 20), 21));

        var response = auditLogService.listAuditLogs(
            9L,
            "  ORDER  ",
            AuditAction.UPDATE,
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 2),
            1,
            20
        );

        verify(auditLogRepository).search(
            eq(9L),
            eq("ORDER"),
            eq(AuditAction.UPDATE),
            createdFromCaptor.capture(),
            createdToCaptor.capture(),
            eq(PageRequest.of(1, 20))
        );

        assertEquals(OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, currentOffset()), createdFromCaptor.getValue());
        assertEquals(OffsetDateTime.of(2026, 4, 3, 0, 0, 0, 0, currentOffset()), createdToCaptor.getValue());
        assertEquals(1, response.page());
        assertEquals(20, response.size());
        assertEquals(21L, response.totalElements());
        assertEquals(2, response.totalPages());
        assertEquals(1, response.items().size());
        assertEquals(5L, response.items().getFirst().id());
        assertEquals("john", response.items().getFirst().operatorUsername());
        assertEquals("WAREHOUSE", response.items().getFirst().operatorRole());
    }

    private ZoneOffset currentOffset() {
        return OffsetDateTime.now().getOffset();
    }
}

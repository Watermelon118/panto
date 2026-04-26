package com.panto.wms.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panto.wms.audit.annotation.Auditable;
import com.panto.wms.audit.domain.AuditAction;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link AuditingAspect} 娴嬭瘯銆?
 */
@ExtendWith(MockitoExtension.class)
class AuditingAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Captor
    private ArgumentCaptor<JsonNode> beforeValueCaptor;

    @Captor
    private ArgumentCaptor<JsonNode> afterValueCaptor;

    @Test
    void auditShouldCaptureSnapshotsWithoutDetachingManagedEntity() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        AuditingAspect auditingAspect = new AuditingAspect(auditLogService, entityManager, objectMapper);
        TestEntity entity = new TestEntity(1L, "old-password-hash", true);
        Method method = TestAuditedMethods.class.getDeclaredMethod("changePassword", Long.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] { 1L });
        when(entityManager.find(TestEntity.class, 1L)).thenReturn(entity);
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            entity.setPasswordHash("new-password-hash");
            entity.setMustChangePassword(false);
            return null;
        });

        auditingAspect.audit(joinPoint, auditable);

        verify(auditLogService).recordAuditLog(
            eq(null),
            eq(null),
            eq(null),
            eq("USER"),
            eq(1L),
            eq(AuditAction.UPDATE),
            eq("淇敼鏈汉瀵嗙爜"),
            eq(null),
            beforeValueCaptor.capture(),
            afterValueCaptor.capture()
        );
        verify(entityManager, never()).detach(any());

        JsonNode beforeValue = beforeValueCaptor.getValue();
        JsonNode afterValue = afterValueCaptor.getValue();

        assertFalse(beforeValue.has("passwordHash"));
        assertFalse(afterValue.has("passwordHash"));
        assertTrue(beforeValue.get("mustChangePassword").asBoolean());
        assertFalse(afterValue.get("mustChangePassword").asBoolean());
        assertEquals(1L, afterValue.get("id").asLong());
    }

    @Test
    void auditShouldAllowCreateMethodsToResolveEntityIdFromResultAfterProceeding() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper();
        AuditingAspect auditingAspect = new AuditingAspect(auditLogService, entityManager, objectMapper);
        TestEntity createdEntity = new TestEntity(8L, "created-password-hash", false);
        Method method = TestAuditedMethods.class.getDeclaredMethod("createInbound");
        Auditable auditable = method.getAnnotation(Auditable.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn(new TestCreateResult(8L));
        when(entityManager.find(TestEntity.class, 8L)).thenReturn(createdEntity);

        auditingAspect.audit(joinPoint, auditable);

        verify(auditLogService).recordAuditLog(
            eq(null),
            eq(null),
            eq(null),
            eq("INBOUND_RECORD"),
            eq(8L),
            eq(AuditAction.CREATE),
            eq("create inbound record"),
            eq(null),
            eq(null),
            afterValueCaptor.capture()
        );

        JsonNode afterValue = afterValueCaptor.getValue();
        assertFalse(afterValue.has("passwordHash"));
        assertEquals(8L, afterValue.get("id").asLong());
        assertFalse(afterValue.get("mustChangePassword").asBoolean());
    }

    private static final class TestAuditedMethods {

        @Auditable(
            action = AuditAction.UPDATE,
            entityType = "USER",
            entityClass = TestEntity.class,
            entityId = "#entityId",
            description = "淇敼鏈汉瀵嗙爜"
        )
        private void changePassword(Long entityId) {
        }

        @Auditable(
            action = AuditAction.CREATE,
            entityType = "INBOUND_RECORD",
            entityClass = TestEntity.class,
            entityId = "#result.id",
            description = "create inbound record"
        )
        private TestCreateResult createInbound() {
            return null;
        }
    }

    private record TestCreateResult(Long id) {
    }

    private static final class TestEntity {

        private Long id;
        private String passwordHash;
        private Boolean mustChangePassword;

        private TestEntity(Long id, String passwordHash, Boolean mustChangePassword) {
            this.id = id;
            this.passwordHash = passwordHash;
            this.mustChangePassword = mustChangePassword;
        }

        public Long getId() {
            return id;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public void setPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        public Boolean getMustChangePassword() {
            return mustChangePassword;
        }

        public void setMustChangePassword(Boolean mustChangePassword) {
            this.mustChangePassword = mustChangePassword;
        }
    }
}

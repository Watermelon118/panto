package com.panto.wms.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.panto.wms.audit.annotation.Auditable;
import com.panto.wms.auth.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 基于注解的审计切面，在写操作成功后自动记录前后快照。
 */
@Slf4j
@Aspect
@Component
public class AuditingAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "passwordHash",
        "currentPassword",
        "newPassword",
        "accessToken",
        "refreshToken"
    );

    private final AuditLogService auditLogService;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 创建审计切面。
     *
     * @param auditLogService 审计日志服务
     * @param entityManager JPA EntityManager
     * @param objectMapper JSON 序列化组件
     */
    public AuditingAspect(
        AuditLogService auditLogService,
        EntityManager entityManager,
        ObjectMapper objectMapper
    ) {
        this.auditLogService = auditLogService;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
    }

    /**
     * 环绕带有 @Auditable 标记的方法，在成功执行后记录审计日志。
     *
     * @param joinPoint 切点
     * @param auditable 注解配置
     * @return 原方法返回值
     * @throws Throwable 原方法异常
     */
    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        EvaluationContext beforeContext = buildContext(joinPoint, null);
        Long beforeEntityId = resolveEntityId(auditable.entityId(), beforeContext);
        JsonNode beforeValue = loadEntitySnapshot(auditable.entityClass(), beforeEntityId);

        Object result = joinPoint.proceed();

        EvaluationContext afterContext = buildContext(joinPoint, result);
        Long afterEntityId = resolveEntityId(auditable.entityId(), afterContext);
        Long entityId = afterEntityId != null ? afterEntityId : beforeEntityId;
        JsonNode afterValue = loadEntitySnapshot(auditable.entityClass(), entityId);
        if (afterValue == null && result != null) {
            afterValue = sanitize(objectMapper.valueToTree(result));
        }

        AuthenticatedUser currentUser = resolveCurrentUser();
        auditLogService.recordAuditLog(
            currentUser != null ? currentUser.getUserId() : null,
            currentUser != null ? currentUser.getUsername() : null,
            currentUser != null ? currentUser.getRole().name() : null,
            auditable.entityType(),
            entityId,
            auditable.action(),
            auditable.description(),
            resolveCurrentIpAddress(),
            beforeValue,
            afterValue
        );
        return result;
    }

    private EvaluationContext buildContext(ProceedingJoinPoint joinPoint, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        context.setVariable("args", args);
        context.setVariable("result", result);
        return context;
    }

    private Long resolveEntityId(String expression, EvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        Object value = expressionParser.parseExpression(expression).getValue(context);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private JsonNode loadEntitySnapshot(Class<?> entityClass, Long entityId) {
        if (entityClass == Void.class || entityId == null) {
            return null;
        }

        Object entity = entityManager.find(entityClass, entityId);
        if (entity == null) {
            return null;
        }

        entityManager.detach(entity);
        return sanitize(objectMapper.valueToTree(entity));
    }

    private JsonNode sanitize(JsonNode node) {
        if (node == null) {
            return null;
        }

        if (node.isObject()) {
            ObjectNode objectNode = ((ObjectNode) node).deepCopy();
            for (String field : SENSITIVE_FIELDS) {
                objectNode.remove(field);
            }

            objectNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode child = objectNode.get(fieldName);
                if (child != null) {
                    objectNode.set(fieldName, sanitize(child));
                }
            });
            return objectNode;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            node.forEach(item -> arrayNode.add(sanitize(item)));
            return arrayNode;
        }

        return node.deepCopy();
    }

    private AuthenticatedUser resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            return null;
        }
        return authenticatedUser;
    }

    private String resolveCurrentIpAddress() {
        HttpServletRequest request = resolveCurrentRequest();
        return request == null ? null : resolveClientIp(request);
    }

    private HttpServletRequest resolveCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

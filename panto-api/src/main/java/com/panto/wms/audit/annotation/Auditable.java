package com.panto.wms.audit.annotation;

import com.panto.wms.audit.domain.AuditAction;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要写入审计日志的业务方法。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * 审计动作。
     *
     * @return 动作类型
     */
    AuditAction action();

    /**
     * 业务对象类型。
     *
     * @return 业务对象类型
     */
    String entityType();

    /**
     * 业务对象 ID 的 SpEL 表达式，可引用方法参数和 result。
     *
     * @return 实体 ID 表达式
     */
    String entityId() default "";

    /**
     * 快照对应的实体类。提供后会在方法前后自动读取数据库快照。
     *
     * @return 实体类
     */
    Class<?> entityClass() default Void.class;

    /**
     * 审计描述。
     *
     * @return 描述
     */
    String description() default "";
}

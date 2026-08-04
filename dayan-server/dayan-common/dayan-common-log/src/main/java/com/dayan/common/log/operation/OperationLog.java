package com.dayan.common.log.operation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解。标注在 Controller/Service 方法上，由 {@link OperationLogAspect} 拦截记录。
 *
 * <p>记录 6 类账号操作（增删改查/审核/导出等），异步落库 system_operation_log。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作模块（如"权益管理"） */
    String module() default "";

    /** 操作类型（如"新增"/"修改"/"删除"/"审核"/"导出"） */
    String action() default "";

    /** 是否记录请求参数（默认 true） */
    boolean logArgs() default true;

    /** 是否记录返回结果（默认 false，敏感接口可关闭） */
    boolean logResult() default false;

    /** 参数脱敏字段名（逗号分隔，如 "password,idCard"） */
    String maskFields() default "";
}

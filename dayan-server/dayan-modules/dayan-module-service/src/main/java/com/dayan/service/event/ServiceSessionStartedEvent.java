package com.dayan.service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 服务会话开始事件。
 *
 * <p>由 {@code ServiceSessionServiceImpl.startService()} 发布，用于跨域联动：
 * 监听方可据此将权益状态从「已激活(2)」流转到「使用中(3)」并维护使用计数。
 *
 * <p>使用 Spring 事件机制而非直接依赖，避免 service ↔ equity 模块循环依赖
 * （equity 已依赖 service 用于激活后创建会话，service 不能反向依赖 equity）。
 */
@Getter
public class ServiceSessionStartedEvent extends ApplicationEvent {

    private final String sessionCode;
    private final String equityCode;
    private final String clientCode;

    public ServiceSessionStartedEvent(Object source, String sessionCode, String equityCode, String clientCode) {
        super(source);
        this.sessionCode = sessionCode;
        this.equityCode = equityCode;
        this.clientCode = clientCode;
    }
}

package com.dayan.service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 服务会话完成事件。
 *
 * <p>由 {@code ServiceSessionServiceImpl.finish()} 发布，用于跨域联动：
 * 监听方可据此回调权益使用计数（use_count+1）、通知客户等。
 *
 * <p>使用 Spring 事件机制而非直接依赖，避免 service ↔ equity 模块循环依赖
 * （equity 已依赖 service 用于激活后创建会话，service 不能反向依赖 equity）。
 */
@Getter
public class ServiceSessionFinishedEvent extends ApplicationEvent {

    private final String sessionCode;
    private final String equityCode;
    private final String clientCode;

    public ServiceSessionFinishedEvent(Object source, String sessionCode, String equityCode, String clientCode) {
        super(source);
        this.sessionCode = sessionCode;
        this.equityCode = equityCode;
        this.clientCode = clientCode;
    }
}

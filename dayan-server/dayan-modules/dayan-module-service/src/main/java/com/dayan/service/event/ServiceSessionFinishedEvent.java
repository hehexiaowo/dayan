package com.dayan.service.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 服务会话完成事件。
 *
 * <p>由 {@code ServiceSessionServiceImpl.finish()} 发布，用于跨域联动：
 * 监听方据此按 equity+item 聚合统计消费次数，判断配额是否全部用尽。
 *
 * <p>使用 Spring 事件机制而非直接依赖，避免 service ↔ equity 模块循环依赖。
 */
@Getter
public class ServiceSessionFinishedEvent extends ApplicationEvent {

    private final String sessionCode;
    private final String equityCode;
    private final String clientCode;
    /** 服务项目编码（listener 据此按 item 聚合统计配额消费） */
    private final String itemCode;
    /** 配额周期（1=终身,2=年度） */
    private final Integer quotaType;

    public ServiceSessionFinishedEvent(Object source, String sessionCode, String equityCode, String clientCode,
                                       String itemCode, Integer quotaType) {
        super(source);
        this.sessionCode = sessionCode;
        this.equityCode = equityCode;
        this.clientCode = clientCode;
        this.itemCode = itemCode;
        this.quotaType = quotaType;
    }
}

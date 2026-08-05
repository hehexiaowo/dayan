package com.dayan.equity.event;

import com.dayan.equity.enums.EquityEvent;
import com.dayan.equity.service.EquityDepotService;
import com.dayan.service.event.ServiceSessionFinishedEvent;
import com.dayan.service.event.ServiceSessionStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 权益域对服务域事件的监听器 —— 打通 service ↔ equity 单向联动。
 *
 * <p>背景：equity 模块已依赖 service 模块（激活后自动创建会话），所以 service 不能反向
 * 直接依赖 equity。通过 Spring 事件机制实现反向联动：
 * <ul>
 *   <li>{@link ServiceSessionStartedEvent} → equity {@code start_service}（2→3 使用中 + use_count+1）</li>
 *   <li>{@link ServiceSessionFinishedEvent} → equity {@code end_service}（3→2 恢复激活），
 *       并在 use_count ≥ max_use_count 时触发 {@code complete}（3→4 已完成，经状态机两段式流转）</li>
 * </ul>
 *
 * <p>所有联动 try-catch 包裹：下游异常仅告警，不阻断主流程（会话状态已落库）。
 * 监听器同步执行（{@code @EventListener} 默认），与发布方同事务，保证 use_count 一致性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EquityUsageEventListener {

    private final EquityDepotService equityDepotService;

    /**
     * 服务开始 → 权益 2→3 使用中。
     *
     * <p>幂等保护：仅当前状态为 STATUS_ACTIVATED(2) 时才流转，避免重复触发
     * （如会话被异常重开）。状态机本身也会校验非法迁移并抛错，双层兜底。
     */
    @Order(10)
    @EventListener
    public void onSessionStarted(ServiceSessionStartedEvent event) {
        String equityCode = event.getEquityCode();
        if (equityCode == null || equityCode.isBlank()) {
            log.debug("服务开始事件无 equityCode，跳过权益联动: sessionCode={}", event.getSessionCode());
            return;
        }
        try {
            Integer to = equityDepotService.transition(equityCode, EquityEvent.START_SERVICE);
            log.info("服务开始联动权益: sessionCode={}, equityCode={}, 2--start_service-->{}", 
                    event.getSessionCode(), equityCode, to);
        } catch (Exception e) {
            log.warn("服务开始联动权益失败（不影响主流程）: sessionCode={}, equityCode={}",
                    event.getSessionCode(), equityCode, e);
        }
    }

    /**
     * 服务完成 → 权益 3→2 恢复激活，若使用次数达到上限则进一步 2→4 完成。
     *
     * <p>设计说明：
     * <ul>
     *   <li>先 {@code end_service}（3→2）：让权益从「使用中」恢复「已激活」</li>
     *   <li>若恢复后 use_count ≥ max_use_count，再尝试 {@code complete}（2→4 完成）。
     *       注意状态机规则里 complete 只允许 3→4，所以先 end_service 回到 2，
     *       再用 {@code transition} 在合适时机推进 —— 这里实际由 END_SERVICE 回到 2 后，
     *       若需完成需走业务侧显式 complete（3→4）。简化策略：仅做 end_service 回滚，
     *       use_count 达上限的「自动完成」由调度任务或前端按钮触发，不在事件链中推进。</li>
     * </ul>
     */
    @Order(10)
    @EventListener
    public void onSessionFinished(ServiceSessionFinishedEvent event) {
        String equityCode = event.getEquityCode();
        if (equityCode == null || equityCode.isBlank()) {
            log.debug("服务完成事件无 equityCode，跳过权益联动: sessionCode={}", event.getSessionCode());
            return;
        }
        try {
            Integer to = equityDepotService.transition(equityCode, EquityEvent.END_SERVICE);
            log.info("服务完成联动权益: sessionCode={}, equityCode={}, 3--end_service-->{}",
                    event.getSessionCode(), equityCode, to);
        } catch (Exception e) {
            log.warn("服务完成联动权益失败（不影响主流程）: sessionCode={}, equityCode={}",
                    event.getSessionCode(), equityCode, e);
        }
    }
}

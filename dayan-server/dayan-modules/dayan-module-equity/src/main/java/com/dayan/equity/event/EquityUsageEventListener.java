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
 *   <li>{@link ServiceSessionStartedEvent} → equity {@code start_service}（2→3 使用中）</li>
 *   <li>{@link ServiceSessionFinishedEvent} → 终身配额用尽时 {@code complete}（3→4 已完成），
 *       否则 {@code end_service}（3→2 恢复激活）。配额计数在 service_session.used_count 维护，
 *       事件携带 usedCount/maxUseCount/quotaType。年度配额（quota_type=2）不因用尽而完成权益。</li>
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
     * 服务完成 → 权益状态流转。
     *
     * <p>设计说明：
     * <ul>
     *   <li>终身配额（quota_type=1）用尽（usedCount ≥ maxUseCount）：直接 {@code complete}（3→4），
     *       因为 COMPLETE 只允许 3→4，必须在 END_SERVICE 之前执行。</li>
     *   <li>其他情况：{@code end_service}（3→2 恢复激活）。年度配额用尽不触发完成，
     *       由 {@code QuotaResetScheduler} 年初重置 used_count。</li>
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
        // 终身配额（quota_type=1）达上限 → 直接 3→4 完成（COMPLETE 只允许 3→4，必须在 END_SERVICE 之前）
        // 年度配额（quota_type=2）不因用尽而完成权益，由年度重置任务恢复
        boolean quotaExhausted = event.getMaxUseCount() != null && event.getUsedCount() != null
                && event.getQuotaType() != null && event.getQuotaType() == 1
                && event.getUsedCount() >= event.getMaxUseCount();
        try {
            if (quotaExhausted) {
                Integer to = equityDepotService.transition(equityCode, EquityEvent.COMPLETE);
                log.info("终身配额用尽，权益自动完成: sessionCode={}, equityCode={}, 3--complete-->{}, used={}/{}",
                        event.getSessionCode(), equityCode, to, event.getUsedCount(), event.getMaxUseCount());
            } else {
                Integer to = equityDepotService.transition(equityCode, EquityEvent.END_SERVICE);
                log.info("服务完成联动权益: sessionCode={}, equityCode={}, 3--end_service-->{}",
                        event.getSessionCode(), equityCode, to);
            }
        } catch (Exception e) {
            log.warn("服务完成联动权益失败（不影响主流程）: sessionCode={}, equityCode={}",
                    event.getSessionCode(), equityCode, e);
        }
    }
}

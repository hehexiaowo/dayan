package com.dayan.equity.event;

import com.dayan.equity.entity.EquityDepot;
import com.dayan.equity.enums.EquityEvent;
import com.dayan.equity.service.EquityDepotService;
import com.dayan.goods.service.GoodsEquityService;
import com.dayan.goods.vo.GoodsEquityVO;
import com.dayan.service.event.ServiceSessionFinishedEvent;
import com.dayan.service.event.ServiceSessionStartedEvent;
import com.dayan.service.mapper.ServiceSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 权益域对服务域事件的监听器 —— 打通 service ↔ equity 单向联动。
 *
 * <p>背景：equity 模块已依赖 service 模块（激活后可查询会话），所以 service 不能反向
 * 直接依赖 equity。通过 Spring 事件机制实现反向联动：
 * <ul>
 *   <li>{@link ServiceSessionStartedEvent} → equity {@code start_service}（2→3 使用中）</li>
 *   <li>{@link ServiceSessionFinishedEvent} → 按 equity+item 聚合判断配额是否全部用尽，
 *       用尽则 {@code complete}（3→4 已完成），否则 {@code end_service}（3→2 恢复激活）。</li>
 * </ul>
 *
 * <p>配额聚合：每次履约 = 1 个 session（finish 后 used_count=1），配额判断按 equity+item
 * 统计已完成会话数对比 rel.quantity。年度配额（quota_type=2）只统计当年消费。
 *
 * <p>所有联动 try-catch 包裹：下游异常仅告警，不阻断主流程（会话状态已落库）。
 * 监听器同步执行（{@code @EventListener} 默认），与发布方同事务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EquityUsageEventListener {

    private final EquityDepotService equityDepotService;
    private final GoodsEquityService goodsEquityService;
    private final ServiceSessionMapper serviceSessionMapper;

    /**
     * 服务开始 → 权益 2→3 使用中。
     *
     * <p>幂等保护：仅当前状态为 STATUS_ACTIVATED(2) 时才流转，避免重复触发。
     * 状态机本身也会校验非法迁移并抛错，双层兜底。
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
     * 服务完成 → 按配额聚合判断权益流转。
     *
     * <p>设计说明：
     * <ul>
     *   <li>查权益的 goodsCode → 查 rel 配置（每个 service_item 的 quantity + quotaType）</li>
     *   <li>对每个 rel，按 equity+item 聚合统计已完成消费次数（年度只数当年）</li>
     *   <li>所有 rel 的消费次数都 ≥ quantity → 全部用尽 → {@code complete}（3→4）</li>
     *   <li>否则 → {@code end_service}（3→2 恢复激活），还有剩余配额</li>
     * </ul>
     *
     * <p>注意：COMPLETE 只允许 3→4，END_SERVICE 只允许 3→2，两者互斥（都要求 from=3），
     * 所以 if-else 分支保证不会出现先 END_SERVICE 变成 2 再 COMPLETE 失败的情况。
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
            // 查权益实体拿 goodsCode，再查 rel 配置
            EquityDepot depot = equityDepotService.requireEquity(equityCode);
            List<GoodsEquityVO.ServiceItemRelVO> rels = goodsEquityService.listRelsByGoodsCode(depot.getGoodsCode());
            if (rels == null || rels.isEmpty()) {
                // 无服务项目配置的权益，完成即结束
                Integer to = equityDepotService.transition(equityCode, EquityEvent.END_SERVICE);
                log.info("权益无服务项目配置，服务完成即恢复: equityCode={}, 3--end_service-->{}", equityCode, to);
                return;
            }

            // 按每个 rel 聚合统计：是否所有项目的配额都已用尽
            int currentYear = LocalDate.now(ZoneId.of("Asia/Shanghai")).getYear();
            boolean allExhausted = true;
            for (GoodsEquityVO.ServiceItemRelVO rel : rels) {
                int quantity = rel.getQuantity() != null ? rel.getQuantity() : 1;
                int quotaType = rel.getQuotaType() != null ? rel.getQuotaType() : 2;
                int consumed;
                if (quotaType == 1) {
                    // 终身配额：统计全部已完成消费
                    consumed = serviceSessionMapper.countConsumedSessions(equityCode, rel.getItemCode());
                } else {
                    // 年度配额：只统计当年消费
                    consumed = serviceSessionMapper.countConsumedSessionsAnnual(
                            equityCode, rel.getItemCode(), currentYear);
                }
                if (consumed < quantity) {
                    allExhausted = false;
                    log.debug("配额未用尽: equityCode={}, itemCode={}, consumed={}, quantity={}, quotaType={}",
                            equityCode, rel.getItemCode(), consumed, quantity, quotaType);
                    break;
                }
            }

            if (allExhausted) {
                Integer to = equityDepotService.transition(equityCode, EquityEvent.COMPLETE);
                log.info("所有服务项目配额用尽，权益自动完成: sessionCode={}, equityCode={}, 3--complete-->{}",
                        event.getSessionCode(), equityCode, to);
            } else {
                Integer to = equityDepotService.transition(equityCode, EquityEvent.END_SERVICE);
                log.info("服务完成联动权益（配额剩余）: sessionCode={}, equityCode={}, 3--end_service-->{}",
                        event.getSessionCode(), equityCode, to);
            }
        } catch (Exception e) {
            log.warn("服务完成联动权益失败（不影响主流程）: sessionCode={}, equityCode={}",
                    event.getSessionCode(), equityCode, e);
        }
    }
}

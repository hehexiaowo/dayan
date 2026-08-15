package com.dayan.equity.event;

import com.dayan.equity.entity.EquityDepot;
import com.dayan.equity.enums.EquityEvent;
import com.dayan.equity.service.EquityDepotService;
import com.dayan.goods.service.GoodsEquityService;
import com.dayan.goods.vo.GoodsEquityVO;
import com.dayan.service.event.ServiceSessionFinishedEvent;
import com.dayan.service.event.ServiceSessionStartedEvent;
import com.dayan.service.mapper.ServiceSessionMapper;
import com.dayan.service.util.QuotaYears;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
 * <p><b>事务边界</b>：使用 {@code @TransactionalEventListener(AFTER_COMMIT)}，在 session
 * 写操作事务提交后独立执行权益联动。这样权益 transition 失败不会回滚已提交的 session
 * 状态（避免 UnexpectedRollbackException），同时也意味着权益联动是"最终一致"——如果联动
 * 失败，session 已完成但权益状态可能短暂不一致（仅 log.warn，需人工补偿）。
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
     *
     * <p>在 session startService 事务提交后执行（AFTER_COMMIT）。
     */
    @Order(10)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
     *   <li>自动完成（complete 3→4）仅由「终身配额」决定：所有终身 rel 用尽才完成。
     *     年度配额（quota_type=2）按激活周年重置，本周年用尽不代表权益终结
     *     （如终身权益旅居6次/年，第1年用满6次，第2年仍可用）——存在任一年度 rel
     *     或按人配额（share_mode=0，按人用尽不代表整卡终结）时，永远走 end_service 恢复激活，
     *     权益终结由过期任务/人工作废驱动。</li>
     *   <li>年度消费按「权益周年序号」统计（anchor=激活时间，QuotaYears.benefitYear），
     *     修复一年期卡跨自然年配额翻倍的问题。</li>
     * </ul>
     *
     * <p>注意：COMPLETE 只允许 3→4，END_SERVICE 只允许 3→2，两者互斥（都要求 from=3），
     * 所以 if-else 分支保证不会出现先 END_SERVICE 变成 2 再 COMPLETE 失败的情况。
     */
    @Order(10)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

            // 按人配额（share_mode=0）：单人不代表整卡，永远恢复激活，不自动完成
            boolean perPerson = depot.getShareMode() != null && depot.getShareMode() == 0;

            // 按每个 rel 聚合统计：所有「终身」配额是否都已用尽（年度配额会重置，不参与完成判定）
            LocalDate anchor = depot.getActivateTime() != null ? depot.getActivateTime().toLocalDate() : null;
            int benefitYear = QuotaYears.benefitYear(anchor, LocalDate.now(ZoneId.of("Asia/Shanghai")));
            boolean hasAnnual = false;
            boolean hasLifetime = false;
            boolean allLifetimeExhausted = true;
            for (GoodsEquityVO.ServiceItemRelVO rel : rels) {
                int quantity = rel.getQuantity() != null ? rel.getQuantity() : 1;
                int quotaType = rel.getQuotaType() != null ? rel.getQuotaType() : 2;
                if (quotaType == 1) {
                    // 终身配额：统计全部已完成消费
                    hasLifetime = true;
                    int consumed = serviceSessionMapper.countConsumedSessions(equityCode, rel.getItemCode());
                    if (consumed < quantity) {
                        allLifetimeExhausted = false;
                        log.debug("终身配额未用尽: equityCode={}, itemCode={}, consumed={}, quantity={}",
                                equityCode, rel.getItemCode(), consumed, quantity);
                    }
                } else {
                    // 年度配额：按激活周年统计（仅用于日志观察，不参与完成判定）
                    hasAnnual = true;
                    int consumedAnnual = serviceSessionMapper.countConsumedSessionsAnnual(
                            equityCode, rel.getItemCode(), benefitYear);
                    if (consumedAnnual < quantity) {
                        log.debug("年度配额未用尽: equityCode={}, itemCode={}, benefitYear={}, consumed={}, quantity={}",
                                equityCode, rel.getItemCode(), benefitYear, consumedAnnual, quantity);
                    }
                }
            }

            boolean shouldComplete = !perPerson && hasLifetime && allLifetimeExhausted && !hasAnnual;
            if (shouldComplete) {
                // 所有终身配额用尽且无年度/按人配额 → complete(3→4)。
                // 权益可能已在2（被前一次finish回退），先 start_service 拉到3
                ensureInUse(equityCode, depot);
                Integer to = equityDepotService.transition(equityCode, EquityEvent.COMPLETE);
                log.info("所有终身配额用尽，权益自动完成: sessionCode={}, equityCode={}, --complete-->{}",
                        event.getSessionCode(), equityCode, to);
            } else {
                // 仍有可重置/未用尽的配额 → end_service(3→2 恢复激活)。权益可能已在2，transition 失败属正常（幂等）
                ensureInUse(equityCode, depot);
                Integer to = equityDepotService.transition(equityCode, EquityEvent.END_SERVICE);
                log.info("服务完成联动权益（配额剩余或可重置）: sessionCode={}, equityCode={}, --end_service-->{}",
                        event.getSessionCode(), equityCode, to);
            }
        } catch (Exception e) {
            log.warn("服务完成联动权益失败（不影响主流程）: sessionCode={}, equityCode={}",
                    event.getSessionCode(), equityCode, e);
        }
    }

    /**
     * 确保权益处于"使用中"(状态3)，以便 END_SERVICE(3→2) 或 COMPLETE(3→4) 能合法执行。
     *
     * <p>场景：多个 session 并行时，前一次 finish 的 END_SERVICE 把权益从3回到2，
     * 当前 finish 的 listener 再执行时权益已在2，直接调 transition(3→x) 会失败。
     * 此方法在权益处于2时先 start_service(2→3) 拉到3，然后再由调用方做 complete/end_service。
     * 已完成(4)/已过期(5)/已作废(6) 等终态不做任何操作（transition 会自然失败被外层catch）。
     */
    private void ensureInUse(String equityCode, EquityDepot depot) {
        Integer status = depot.getEquityStatus();
        if (status != null && status == EquityEvent.STATUS_ACTIVATED) {
            equityDepotService.transition(equityCode, EquityEvent.START_SERVICE);
            log.debug("权益从已激活拉到使用中: equityCode={}, 2--start_service-->3", equityCode);
        }
    }
}

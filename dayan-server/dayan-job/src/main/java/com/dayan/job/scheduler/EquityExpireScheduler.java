package com.dayan.job.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.equity.entity.EquityDepot;
import com.dayan.equity.enums.EquityEvent;
import com.dayan.equity.mapper.EquityDepotMapper;
import com.dayan.equity.service.EquityDepotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权益过期扫描定时任务。
 *
 * <p>每小时扫描两类过期权益，经 EQUITY_SM 状态机合法转移至"已过期"（状态 5）：
 * <ul>
 *   <li><b>上架过期（shelf_expire）</b>：equity_status=0（库存中）且 shelf_expire_time &lt; 当前时间
 *       → 事件 {@code shelf_expire}（0→5）</li>
 *   <li><b>有效期过期（expire）</b>：equity_status ∈ {1,2}（已出库/已激活）且 expire_time &lt; 当前时间
 *       → 事件 {@code expire}（1→5 / 2→5）</li>
 * </ul>
 *
 * <p>分批处理（每批 {@link #BATCH_SIZE} 条），逐条经 {@link EquityDepotService#transition} 走状态机，
 * 保证每条流转合法并联动批次统计。单条失败仅记录日志、不中断整批。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EquityExpireScheduler {

    /** 单批处理条数（避免大事务、控制内存） */
    private static final int BATCH_SIZE = 500;

    private final EquityDepotMapper equityDepotMapper;
    private final EquityDepotService equityDepotService;

    /**
     * 每小时整点执行（cron：分 时 日 月 周）。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scanExpiredEquity() {
        long start = System.currentTimeMillis();
        int shelfExpired = scanShelfExpired();
        int expired = scanActiveExpired();
        int total = shelfExpired + expired;
        log.info("[权益过期扫描] 完成，上架过期 {} 条、有效期过期 {} 条、合计 {} 条，耗时 {}ms",
                shelfExpired, expired, total, System.currentTimeMillis() - start);
    }

    /**
     * 扫描库存中（status=0）且上架有效期已过的权益。
     */
    private int scanShelfExpired() {
        LocalDateTime now = LocalDateTime.now();
        int processed = 0;
        while (true) {
            List<EquityDepot> list = equityDepotMapper.selectList(
                    new LambdaQueryWrapper<EquityDepot>()
                            .eq(EquityDepot::getEquityStatus, EquityEvent.STATUS_STOCK)
                            .lt(EquityDepot::getShelfExpireTime, now)
                            .last("LIMIT " + BATCH_SIZE));
            if (list.isEmpty()) {
                break;
            }
            for (EquityDepot equity : list) {
                try {
                    equityDepotService.transition(equity.getEquityCode(), EquityEvent.SHELF_EXPIRE);
                    processed++;
                } catch (Exception e) {
                    log.warn("[权益过期扫描-上架] equityCode={} 转移失败: {}",
                            equity.getEquityCode(), e.getMessage());
                }
            }
            if (list.size() < BATCH_SIZE) {
                break;
            }
        }
        return processed;
    }

    /**
     * 扫描已出库/已激活（status ∈ {1,2}）且有效期已过的权益。
     */
    private int scanActiveExpired() {
        LocalDateTime now = LocalDateTime.now();
        int processed = 0;
        while (true) {
            List<EquityDepot> list = equityDepotMapper.selectList(
                    new LambdaQueryWrapper<EquityDepot>()
                            .in(EquityDepot::getEquityStatus,
                                    EquityEvent.STATUS_OUTBOUND, EquityEvent.STATUS_ACTIVATED)
                            .isNotNull(EquityDepot::getExpireTime)
                            .lt(EquityDepot::getExpireTime, now)
                            .last("LIMIT " + BATCH_SIZE));
            if (list.isEmpty()) {
                break;
            }
            for (EquityDepot equity : list) {
                try {
                    equityDepotService.transition(equity.getEquityCode(), EquityEvent.EXPIRE);
                    processed++;
                } catch (Exception e) {
                    log.warn("[权益过期扫描-有效期] equityCode={} 转移失败: {}",
                            equity.getEquityCode(), e.getMessage());
                }
            }
            if (list.size() < BATCH_SIZE) {
                break;
            }
        }
        return processed;
    }
}

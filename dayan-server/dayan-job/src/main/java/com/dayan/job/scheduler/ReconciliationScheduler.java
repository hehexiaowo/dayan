package com.dayan.job.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 自动对账定时任务。
 *
 * <p>每日 03:00 执行，核对前一日流水与结算单一致性，差异告警。
 * P0 占位：仅记录日志；P7 结算域开发后接入 FinanceReconciliationMapper 真实逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyReconciliation() {
        long start = System.currentTimeMillis();
        try {
            // TODO P7: 注入 FinanceFlowMapper / FinanceBillMapper，对账前一日数据
            int diffCount = 0;
            log.info("[自动对账] 对账完成，差异 {} 条，耗时 {}ms", diffCount, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[自动对账] 执行失败", e);
        }
    }
}

package com.dayan.job.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 权益过期扫描定时任务。
 *
 * <p>每小时扫描已过期权益（expire_time < 当前时间），状态置为 5（已过期）。
 * P0 占位：仅记录日志；P4 权益域开发后接入 EquityDepotMapper 真实逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EquityExpireScheduler {

    /**
     * 每小时执行一次（cron：分 时 日 月 周）。
     * 0 0 * * * * = 每个整点。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scanExpiredEquity() {
        long start = System.currentTimeMillis();
        try {
            // TODO P4: 注入 EquityDepotMapper，执行 UPDATE equity_depot SET equity_status=5
            //          WHERE equity_status IN (0,1,2) AND expire_time < NOW()
            int count = 0; // 占位
            log.info("[权益过期扫描] 扫描完成，处理 {} 条过期权益，耗时 {}ms", count, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[权益过期扫描] 执行失败", e);
        }
    }
}

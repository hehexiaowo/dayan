package com.dayan.job.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时取消定时任务。
 *
 * <p>每 5 分钟扫描待支付订单（status=0 且创建超 30 分钟），自动取消（status=4）。
 * P0 占位：仅记录日志；P7 订单域开发后接入 OrderMapper 真实逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelTimeoutOrders() {
        long start = System.currentTimeMillis();
        try {
            // TODO P7: 注入 OrderEquityMapper 等，执行
            //   UPDATE order_equity SET status=4 WHERE status=0 AND created_at < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
            int count = 0;
            log.info("[订单超时取消] 扫描完成，取消 {} 笔超时订单，耗时 {}ms", count, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("[订单超时取消] 执行失败", e);
        }
    }
}

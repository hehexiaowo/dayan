package com.dayan.job.scheduler;

import com.dayan.service.mapper.ServiceSessionMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 年度配额重置定时任务。
 *
 * <p><b>当前实现为空操作</b>（不影响数据）。年度配额的隔离靠 session 创建时的
 * quota_reset_year 快照 + {@code countConsumedSessionsAnnual} 按 quota_reset_year = 当前年
 * 统计天然实现——新的一年创建的 session 自带新年份，去年的消费不会被今年的统计匹配。
 *
 * <p>此任务保留为扩展点：未来可用于归档去年数据、清理超期未使用的年度会话等。
 *
 * <p>cron 每年1月1日 00:00 执行（秒 分 时 日 月 周）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaResetScheduler {

    private final ServiceSessionMapper serviceSessionMapper;

    /**
     * 每年1月1日 00:00 执行。
     */
    @Scheduled(cron = "0 0 0 1 1 ?")
    public void resetAnnualQuota() {
        int year = LocalDate.now(ZoneId.of("Asia/Shanghai")).getYear();
        long start = System.currentTimeMillis();
        int affected = serviceSessionMapper.resetAnnualQuota(year);
        log.info("[年度配额重置] 执行完成，年份={}，受影响行数={}，耗时={}ms",
                year, affected, System.currentTimeMillis() - start);
    }

    /**
     * 启动检查（预留扩展点，当前为空操作）。
     */
    @PostConstruct
    public void checkResetOnStartup() {
        int year = LocalDate.now(ZoneId.of("Asia/Shanghai")).getYear();
        int affected = serviceSessionMapper.resetAnnualQuota(year);
        if (affected > 0) {
            log.info("[年度配额重置-启动补执行] 受影响 {} 行，年份={}", affected, year);
        }
    }
}

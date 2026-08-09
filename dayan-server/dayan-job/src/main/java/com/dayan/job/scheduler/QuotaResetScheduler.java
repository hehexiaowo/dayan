package com.dayan.job.scheduler;

import com.dayan.service.mapper.ServiceSessionMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 年度配额重置定时任务。
 *
 * <p>每年1月1日 00:00 执行，将所有年度配额（quota_type=2）的活跃服务会话 used_count 归零，
 * 实现"6次/年"这类年度可循环配额的自动重置。
 *
 * <p>安全机制：{@link #checkResetOnStartup()} 在服务启动时也检查一次 —— 如果跨年重启
 * （例如系统在1月2日重启而定时任务因停机未执行），启动时补执行重置，避免漏重置。
 * 正常情况下启动检查是空操作（quota_reset_year 已是当前年）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaResetScheduler {

    private final ServiceSessionMapper serviceSessionMapper;

    /**
     * 每年1月1日 00:00 执行（cron：秒 分 时 日 月 周）。
     */
    @Scheduled(cron = "0 0 0 1 1 ?")
    public void resetAnnualQuota() {
        int year = LocalDate.now().getYear();
        long start = System.currentTimeMillis();
        int affected = serviceSessionMapper.resetAnnualQuota(year);
        log.info("[年度配额重置] 完成，重置 {} 条年度配额会话的 used_count→0，年份={}，耗时={}ms",
                affected, year, System.currentTimeMillis() - start);
    }

    /**
     * 启动检查：如果跨年未重置则补执行。
     *
     * <p>场景：系统在年初停机维护，定时任务未触发，恢复后需补重置。
     * 正常重启时 quota_reset_year 已是当前年，mapper 查询条件不匹配，affected=0，无副作用。
     */
    @PostConstruct
    public void checkResetOnStartup() {
        int year = LocalDate.now().getYear();
        int affected = serviceSessionMapper.resetAnnualQuota(year);
        if (affected > 0) {
            log.info("[年度配额重置-启动补执行] 跨年未重置，已补重置 {} 条，年份={}", affected, year);
        }
    }
}

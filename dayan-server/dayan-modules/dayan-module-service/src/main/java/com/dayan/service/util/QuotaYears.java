package com.dayan.service.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 权益周年计算（年度配额 quota_type=2 的重置锚点）。
 *
 * <p>年度配额按「激活周年」而非自然年重置：激活日当天起每满 12 个月为一个新的
 * 权益年（第 1 个权益年从激活日开始）。一年期卡在权益期内只会经历 1 个权益年，
 * 修复了按自然年统计导致的跨年配额翻倍问题（2026-09 激活的一年期卡在
 * 2027-01 会被自然年口径重置，实际拿到 20 次）。
 *
 * <p>service_session.quota_reset_year 存的就是本方法计算出的周年序号。
 */
public final class QuotaYears {

    private QuotaYears() {
    }

    /**
     * 计算某日期处于激活后第几个权益年（1 起）。
     *
     * @param anchor 激活日期（周年锚点）
     * @param date   目标日期（通常为今天）
     * @return 权益周年序号；anchor 为 null 时退化为自然年年份（兼容旧数据）
     */
    public static int benefitYear(LocalDate anchor, LocalDate date) {
        if (anchor == null) {
            return date.getYear();
        }
        long completeYears = ChronoUnit.YEARS.between(anchor, date);
        return (int) completeYears + 1;
    }
}

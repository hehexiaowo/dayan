package com.dayan.job.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dayan.park.entity.ParkPricing;
import com.dayan.park.mapper.ParkPricingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 定价预约生效扫描（规格 §F.3）：每小时 :05 执行。
 *
 * 扫 pending_flag=1 且 effective_date<=今天 的待生效调价，逐条同事务执行：
 * 同维度（park|charge_type|ref_code|billing_cycle）旧当前价置 0 → 本条置 1 且 pending_flag=0。
 * 单条失败记日志不中断。expire_date 到期不自动失效（避免无价可用），仅日志告警。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PricingEffectiveScheduler {

    private final ParkPricingMapper parkPricingMapper;

    @Scheduled(cron = "0 5 * * * ?", zone = "Asia/Shanghai")
    @Transactional(rollbackFor = Exception.class)
    public void flipPendingPricing() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        warnExpiredCurrent(today);
        List<ParkPricing> pendingList = parkPricingMapper.selectList(new LambdaQueryWrapper<ParkPricing>()
                .eq(ParkPricing::getPendingFlag, 1)
                .le(ParkPricing::getEffectiveDate, today)
                .orderByAsc(ParkPricing::getId));
        if (pendingList.isEmpty()) {
            return;
        }
        int flipped = 0;
        for (ParkPricing pending : pendingList) {
            try {
                flipOne(pending);
                flipped++;
            } catch (Exception e) {
                log.error("[定价生效] 翻转失败 id={}, parkCode={}, refCode={}",
                        pending.getId(), pending.getParkCode(), pending.getRefCode(), e);
            }
        }
        log.info("[定价生效] 扫描待生效 {} 条，成功翻转 {} 条", pendingList.size(), flipped);
    }

    /** 过期告警（规格 §F.3）：expire_date 已过但仍为当前价 → 仅日志，不自动失效（避免无价可用） */
    private void warnExpiredCurrent(LocalDate today) {
        List<ParkPricing> expired = parkPricingMapper.selectList(new LambdaQueryWrapper<ParkPricing>()
                .eq(ParkPricing::getIsCurrent, 1)
                .isNotNull(ParkPricing::getExpireDate)
                .lt(ParkPricing::getExpireDate, today));
        for (ParkPricing p : expired) {
            log.warn("[定价过期] 当前价已过失效日期，继续沿用直至调价：id={}, parkCode={}, refCode={}, expireDate={}",
                    p.getId(), p.getParkCode(), p.getRefCode(), p.getExpireDate());
        }
    }

    private void flipOne(ParkPricing pending) {
        // 1. 同维度旧当前价置 0（uk_current 生成列要求同维度仅一个当前价）
        parkPricingMapper.update(null, new LambdaUpdateWrapper<ParkPricing>()
                .eq(ParkPricing::getParkCode, pending.getParkCode())
                .eq(ParkPricing::getChargeType, pending.getChargeType())
                .eq(ParkPricing::getRefCode, pending.getRefCode())
                .eq(ParkPricing::getBillingCycle, pending.getBillingCycle())
                .eq(ParkPricing::getIsCurrent, 1)
                .ne(ParkPricing::getId, pending.getId())
                .set(ParkPricing::getIsCurrent, 0));
        // 2. 本条置当前并清 pending
        parkPricingMapper.update(null, new LambdaUpdateWrapper<ParkPricing>()
                .eq(ParkPricing::getId, pending.getId())
                .set(ParkPricing::getIsCurrent, 1)
                .set(ParkPricing::getPendingFlag, 0));
    }
}

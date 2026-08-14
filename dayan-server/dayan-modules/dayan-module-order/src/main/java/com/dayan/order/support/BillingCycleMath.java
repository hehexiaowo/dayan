package com.dayan.order.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 定价周期折算（订单服务端权威取价口径）。
 *
 * 日价 = 周期价 / 周期天数（月30/季90/半年180/年365；周期5=一次性不折算）。
 * 权威费用 = 一次性 ? 周期价 : 日价 × max(stayDays, 1)。金额保留 2 位四舍五入。
 */
public final class BillingCycleMath {

    /** billing_cycle → 周期天数（1月/2季/3半年/4年/5一次性） */
    public static int cycleDays(Integer billingCycle) {
        if (billingCycle == null) return 30;
        return switch (billingCycle) {
            case 2 -> 90; case 3 -> 180; case 4 -> 365; case 5 -> 0; default -> 30;
        };
    }

    /** 周期价折算为订单费用；billingCycle=5（一次性）原样返回 */
    public static BigDecimal toOrderAmount(BigDecimal cyclePrice, Integer billingCycle, int stayDays) {
        if (cyclePrice == null) return null;
        int days = cycleDays(billingCycle);
        if (days == 0) {
            return cyclePrice.setScale(2, RoundingMode.HALF_UP);
        }
        int effectiveDays = Math.max(stayDays, 1);
        return cyclePrice.multiply(BigDecimal.valueOf(effectiveDays))
                .divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    /** 偏差判定：|客户端-权威| > max(1.00, 权威×1%) */
    public static boolean isDeviated(BigDecimal clientValue, BigDecimal authorityValue) {
        if (authorityValue == null) return false;
        BigDecimal client = clientValue == null ? BigDecimal.ZERO : clientValue;
        BigDecimal diff = client.subtract(authorityValue).abs();
        BigDecimal threshold = authorityValue.multiply(new BigDecimal("0.01"))
                .max(new BigDecimal("1.00"));
        return diff.compareTo(threshold) > 0;
    }

    private BillingCycleMath() {}
}

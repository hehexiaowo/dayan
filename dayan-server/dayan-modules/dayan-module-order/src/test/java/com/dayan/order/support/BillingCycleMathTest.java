package com.dayan.order.support;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BillingCycleMathTest {

    @Test
    void 月价按30天折算() {
        assertThat(BillingCycleMath.toOrderAmount(new BigDecimal("6000"), 1, 15))
                .isEqualByComparingTo("3000.00");
    }

    @Test
    void 年价按365天折算() {
        assertThat(BillingCycleMath.toOrderAmount(new BigDecimal("73000"), 4, 10))
                .isEqualByComparingTo("2000.00");
    }

    @Test
    void 一次性不折算() {
        assertThat(BillingCycleMath.toOrderAmount(new BigDecimal("1500"), 5, 15))
                .isEqualByComparingTo("1500.00");
    }

    @Test
    void stayDays零按一天() {
        assertThat(BillingCycleMath.toOrderAmount(new BigDecimal("3000"), 1, 0))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void 偏差阈值_超过1元或1percent() {
        // 权威 100：1% = 1 元，阈值取 max(1,1)=1；diff=1.50 > 1 → 偏差
        assertThat(BillingCycleMath.isDeviated(new BigDecimal("101.50"), new BigDecimal("100.00"))).isTrue();
        assertThat(BillingCycleMath.isDeviated(new BigDecimal("100.00"), new BigDecimal("100.50"))).isFalse();
        // 权威 200：1% = 2 元，阈值取 max(1,2)=2
        assertThat(BillingCycleMath.isDeviated(new BigDecimal("202.01"), new BigDecimal("200"))).isTrue();
        assertThat(BillingCycleMath.isDeviated(new BigDecimal("202.00"), new BigDecimal("200"))).isFalse();
    }

    @Test
    void 空客户端按零参与比对() {
        assertThat(BillingCycleMath.isDeviated(null, new BigDecimal("5.00"))).isTrue();
    }
}

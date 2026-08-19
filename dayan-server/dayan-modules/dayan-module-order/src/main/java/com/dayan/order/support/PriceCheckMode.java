package com.dayan.order.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 订单取价校验模式：strict=偏差拒单；warn=告警并按权威价入账；off=完全采信客户端（回滚开关） */
public enum PriceCheckMode {

    STRICT, WARN, OFF;

    private static final Logger log = LoggerFactory.getLogger(PriceCheckMode.class);

    public static PriceCheckMode parse(String value) {
        if (value == null || value.isBlank()) return STRICT;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[PriceCheckMode] 非法配置值: {}，回退 STRICT", value);
            return STRICT;
        }
    }
}

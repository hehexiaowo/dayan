package com.dayan.order.support;

/** 订单取价校验模式：strict=偏差拒单；warn=告警并按权威价入账；off=完全采信客户端（回滚开关） */
public enum PriceCheckMode {
    STRICT, WARN, OFF;

    public static PriceCheckMode parse(String value) {
        if (value == null || value.isBlank()) return STRICT;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STRICT;
        }
    }
}

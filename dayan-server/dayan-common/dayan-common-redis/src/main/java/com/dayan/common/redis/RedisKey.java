package com.dayan.common.redis;

/**
 * Redis Key 前缀规范。
 *
 * <p>统一前缀 {@code dayan:{module}:{biz}:{id}}，便于按模块扫描与清理。
 */
public final class RedisKey {

    private RedisKey() {
    }

    /** 根前缀 */
    public static final String PREFIX = "dayan";

    /** 业务编码序列：dayan:code:seq:{prefix}:{channelCode} */
    public static final String CODE_SEQ = PREFIX + ":code:seq";

    /** 状态机规则：dayan:sm:rule:{domain}（Hash，field={from}:{event}） */
    public static final String SM_RULE = PREFIX + ":sm:rule";

    /** 登录失败计数：dayan:auth:fail:{accountType}:{loginKey} */
    public static final String AUTH_FAIL = PREFIX + ":auth:fail";

    /** Token 会话：dayan:auth:token:{accountType}（Sa-Token 内部管理） */
    public static final String AUTH_TOKEN = PREFIX + ":auth:token";

    public static String codeSeq(String prefix, long channelCode) {
        return CODE_SEQ + ":" + prefix + ":" + channelCode;
    }

    public static String smRule(String domain) {
        return SM_RULE + ":" + domain;
    }

    public static String authFail(String accountType, String loginKey) {
        return AUTH_FAIL + ":" + accountType + ":" + loginKey;
    }
}

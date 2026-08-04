package com.dayan.common.core.code;

/**
 * 序列号提供者抽象。
 *
 * <p>生产环境由 dayan-common-redis 提供基于 Redis INCR 的实现（并发安全、跨实例唯一）；
 * 测试环境可用内存实现。{@link CodeGenerator} 仅依赖本接口，与具体存储解耦。
 */
public interface SequenceProvider {

    /**
     * 按 key 自增并返回新值（从 1 开始）。
     *
     * @param key 序列键（由调用方拼装，需保证不同业务维度隔离）
     * @return 自增后的值
     */
    long next(String key);
}

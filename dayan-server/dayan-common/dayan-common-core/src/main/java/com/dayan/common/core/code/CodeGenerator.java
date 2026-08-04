package com.dayan.common.core.code;

/**
 * 业务编码生成器。
 *
 * <p>生成形如 {@code PK00001}、{@code AG00002} 的业务编码：
 * 前缀（域标识）+ 按前缀与渠道维度隔离的自增序列（左侧补零）。
 *
 * <p>并发安全：序列号来源 {@link SequenceProvider}（生产环境为 Redis INCR）。
 * 渠道维度：相同前缀在不同 {@code channelCode} 下各自独立计数，满足"渠道内唯一"约束。
 */
public class CodeGenerator {

    /** 默认序列宽度（不含前缀） */
    public static final int DEFAULT_WIDTH = 5;

    private final SequenceProvider sequenceProvider;
    private final int width;

    public CodeGenerator(SequenceProvider sequenceProvider) {
        this(sequenceProvider, DEFAULT_WIDTH);
    }

    /**
     * @param width 序列数字部分宽度（不足左侧补零，超出则自然扩展）
     */
    public CodeGenerator(SequenceProvider sequenceProvider, int width) {
        if (width < 1) {
            throw new IllegalArgumentException("width 必须 >= 1: " + width);
        }
        this.sequenceProvider = sequenceProvider;
        this.width = width;
    }

    /**
     * 生成全局（跨渠道）唯一编码。
     *
     * @param prefix 业务前缀，如 {@code "PK"}、{@code "SP"}
     */
    public String generate(String prefix) {
        return generate(prefix, 0L);
    }

    /**
     * 生成渠道内唯一编码。
     *
     * @param prefix       业务前缀
     * @param channelCode  渠道编码；{@code <=0} 表示全局共享计数
     */
    public String generate(String prefix, long channelCode) {
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("prefix 不能为空");
        }
        String key = buildKey(prefix, channelCode);
        long seq = sequenceProvider.next(key);
        return prefix + pad(seq);
    }

    private String buildKey(String prefix, long channelCode) {
        return "code:seq:" + prefix + ":" + channelCode;
    }

    private String pad(long seq) {
        String s = Long.toString(seq);
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder(width);
        int zeros = width - s.length();
        for (int i = 0; i < zeros; i++) {
            sb.append('0');
        }
        return sb.append(s).toString();
    }
}

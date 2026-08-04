package com.dayan.common.core.code;

/**
 * 业务编码前缀常量（按业务域）。
 *
 * <p>各域 Entity 的 {@code xxx_code} 业务编码统一使用对应前缀 + 序列号，
 * 由 {@link CodeGenerator} 生成，规则见《数据库设计文档》各域编码约定。
 */
public final class BusinessCode {

    private BusinessCode() {
    }

    /** 权益 EQ+12 位 */
    public static final String EQUITY = "EQ";
    /** 供应商 SP+5 位 */
    public static final String SUPPLIER = "SP";
    /** 养老机构 PK+5 位 */
    public static final String PARK = "PK";
    /** 客户 CL+5 位 */
    public static final String CLIENT = "CL";
    /** 代理人 AG+5 位 */
    public static final String AGENT = "AG";
    /** 分销商 DS+5 位 */
    public static final String DISTRIBUTOR = "DS";
    /** 服务会话 session_code */
    public static final String SERVICE_SESSION = "SS";
    /** 订单 OD */
    public static final String ORDER = "OD";
    /** 财务流水 FL */
    public static final String FINANCE_FLOW = "FL";
    /** 商品 SPU */
    public static final String GOODS = "GD";
    /** 场景 */
    public static final String SCENE = "SC";
    /** 课程 */
    public static final String COURSE = "CR";
    /** 内容 */
    public static final String CONTENT = "CT";
    /** 管家 */
    public static final String BUTLER = "BT";
    /** 组织 */
    public static final String ORGAN = "OR";
    /** 渠道 */
    public static final String CHANNEL = "CH";
}

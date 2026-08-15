package com.dayan.goods.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单次服务使用规则（goods_service_item_rel.usage_rule JSON 的结构化形态）。
 *
 * <p>旅居随心住类权益的入住规则（对照文档）：
 * 最多3天2晚、1间房、每间可住2人、提前15天预订、预定金500元、
 * 72h全退/48h退50%/24h不退、春节9天（除夕至初八）不可入住、权益人本人到场。
 *
 * <p>注意：这是「单次服务」的使用人数/间数/晚数，与 goods_equity.person_count
 * （权益人数量）是两个概念——随心住权益人仅本人，但单次可入住2人。
 */
@Data
public class UsageRule {

    /** 每次最多天数（3天2晚 → 3） */
    private Integer maxDaysPerUse;

    /** 每次最多晚数（3天2晚 → 2） */
    private Integer maxNightsPerUse;

    /** 每次1间房 */
    private Integer maxRoomsPerUse;

    /** 每间房可住人数 */
    private Integer maxGuestsPerUse;

    /** 是否须权益人本人到场办理 */
    private Boolean requireBeneficiaryCheckIn;

    /** 需提前预订天数 */
    private Integer advanceBookDays;

    /** 每次预订预定金（元） */
    private BigDecimal depositAmount;

    /** 取消退预定金政策（按距入住小时数分档） */
    private List<RefundRule> refundPolicy;

    /** 不可入住时段类型（spring_festival=春节） */
    private String blackoutType;

    /** 不可入住天数 */
    private Integer blackoutDays;

    /**
     * 取消政策档位：距入住 beforeHours 小时前取消，退预定金 refundRate%。
     */
    @Data
    public static class RefundRule {
        /** 距入住小时数门槛（如 72/48/24） */
        private Integer beforeHours;
        /** 退还比例（0~100） */
        private Integer refundRate;
    }
}

package com.dayan.equity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 权益卡/函（equity_depot）VO。
 *
 * <p>注意：{@code cardSecret} 为 AES-GCM 加密密文，出参默认不回传（脱敏占位）；
 * {@code activateCode}/{@code bindCode} 在管理端可见。
 */
@Data
public class EquityDepotVO {

    private Long id;
    private String equityCode;
    private String equityNo;
    private String goodsCode;
    /** 使用人人数快照 */
    private Integer personCount;
    /** 激活后有效天数快照 */
    private Integer validDays;
    private String batchCode;
    private BigDecimal costPrice;
    private String channelCode;
    private String agentCode;
    private String clientCode;
    private LocalDateTime produceTime;
    private LocalDateTime allocateTime;
    private String outboundChannelCode;
    private String outboundAgentCode;
    private LocalDateTime outboundTime;
    private String logisticsNo;
    private LocalDateTime activateTime;
    private LocalDateTime firstUseTime;
    private LocalDateTime lastUseTime;
    private LocalDateTime expireTime;
    private LocalDateTime shelfExpireTime;
    /** 卡密（脱敏） */
    private String cardSecret;
    /** 载体类型：1=权益卡 / 2=权益函 */
    private Integer carrierType;
    /** 激活码（DY-8位，权益卡专用） */
    private String activateCode;
    /** 绑定码（BF-12位，权益函专用） */
    private String bindCode;
    private String qrCodeUrl;
    private String orderCode;
    /** 商品名称（关联 order_equity 快照，orderCode 为空时为 null） */
    private String goodsName;
    /** 商品规格（关联 order_equity 快照，orderCode 为空时为 null） */
    private String skuName;
    /** 权益状态：0=库存中 / 1=已出库 / 2=已激活 / 3=使用中 / 4=已完成 / 5=已过期 / 6=已作废 / 7=更换权益人中 */
    private Integer equityStatus;
    private String voidReason;
    private String remark;
    private LocalDateTime createdAt;
}

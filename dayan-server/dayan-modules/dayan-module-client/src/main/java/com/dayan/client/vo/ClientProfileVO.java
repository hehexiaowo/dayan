package com.dayan.client.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Client 端个人资料聚合视图（client_info + 渠道/区划名称）。
 *
 * <p>敏感字段服务端脱敏：phone 中间四位打码、idCard 中间打码，仅用于展示。
 */
@Data
@Builder
public class ClientProfileVO {

    /** 客户编码 */
    private String clientCode;
    /** 渠道编码 */
    private String channelCode;
    /** 渠道简称（回填 channel_info.short_name，缺失则用 full_name） */
    private String channelName;

    // ===== 基本信息 =====
    private String fullName;
    /** 性别：0 保密 / 1 男 / 2 女 */
    private Integer gender;
    /** 头像 OSS key */
    private String avatar;
    /** 手机号（脱敏：138****1234） */
    private String phone;
    private String email;
    private LocalDate birthday;
    private Integer age;
    /** 身份证号（脱敏：保留前 6 后 4，中间打码） */
    private String idCard;

    // ===== 地区 =====
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String provinceName;
    private String cityName;
    private String districtName;
    private String address;

    // ===== 账户 =====
    /** 客户等级 */
    private Integer clientLevel;
    /** 是否 VIP：0 否 / 1 是 */
    private Integer isVip;
    private LocalDateTime registerTime;
    private LocalDateTime lastLoginTime;

    // ===== 资产与统计 =====
    private Integer equityCount;
    private Integer usedEquityCount;
    private Integer serviceCount;
    private BigDecimal totalOrderAmount;
    private LocalDateTime lastServiceTime;
}

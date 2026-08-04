package com.dayan.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 supplier_open_platform 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier_open_platform")
public class SupplierOpenPlatform extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 供应商编码 */
    private String supplierCode;

    /** 平台名称 */
    private String platformName;

    /** API基础地址 */
    private String apiBaseUrl;

    /** 应用Key */
    private String appKey;

    /** 应用密钥 */
    private String appSecret;

    /** 回调地址 */
    private String callbackUrl;

    /** Webhook密钥 */
    private String webhookSecret;

    /** 协议类型 */
    private Integer protocolType;

    /** 认证方式 */
    private Integer authType;

    /** 数据格式 */
    private Integer dataFormat;

    /** API版本 */
    private String apiVersion;

    /** 调用频率限制 */
    private Integer rateLimit;

    /** 超时时间(秒) */
    private Integer timeout;

    /** 重试次数 */
    private Integer retryCount;

    /** 扩展配置 */
    private String extraConfig;

    /** 状态 */
    private Integer status;
}

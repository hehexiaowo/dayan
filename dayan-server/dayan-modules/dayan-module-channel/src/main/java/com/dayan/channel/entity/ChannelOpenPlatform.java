package com.dayan.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 channel_open_platform 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("channel_open_platform")
public class ChannelOpenPlatform extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 渠道编码 */
    private String channelCode;

    /** 平台名称 */
    private String platformName;

    /** 对接类型 */
    private Integer dockType;

    /** API基础地址 */
    private String apiBaseUrl;

    /** 应用Key */
    private String appKey;

    /** 应用密钥 */
    private String appSecret;

    /** 回调地址 */
    private String callbackUrl;

    /** H5域名配置 */
    private String h5Domain;

    /** H5主题配置 */
    private String h5Theme;

    /** 认证方式 */
    private Integer authType;

    /** IP白名单 */
    private String ipWhitelist;

    /** 调用频率限制 */
    private Integer rateLimit;

    /** 超时时间(秒) */
    private Integer timeout;

    /** 扩展配置 */
    private String extraConfig;

    /** 状态 */
    private Integer status;
}

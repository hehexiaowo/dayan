package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 system_message 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_message")
public class SystemMessage extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 消息实例编码 */
    private String messageCode;

    /** 发送批次编码 */
    private String batchCode;

    /** 模板编码 */
    private String templateCode;

    /** 业务类型 */
    private String bizType;

    /** 实际发送渠道 */
    private Integer channelType;

    /** 消息类型 */
    private Integer messageType;

    /** 消息标题 */
    private String title;

    /** 消息正文 */
    private String content;

    /** 接收者类型 */
    private String targetType;

    /** 接收者编码 */
    private String targetCode;

    /** 接收者名称 */
    private String targetName;

    /** 接收者联系方式 */
    private String targetContact;

    /** 发送者类型 */
    private String senderType;

    /** 发送者编码 */
    private String senderCode;

    /** 跳转链接 */
    private String linkUrl;

    /** 链接类型 */
    private Integer linkType;

    /** 发送状态 */
    private Integer sendStatus;

    /** 第三方服务商消息ID */
    private String providerMsgId;

    /** 实际发送时间 */
    private LocalDateTime sendTime;

    /** 送达时间 */
    private LocalDateTime deliverTime;

    /** 已读时间 */
    private LocalDateTime readTime;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 重试次数 */
    private Integer retryCount;

    /** 失败错误码 */
    private String errorCode;

    /** 失败原因 */
    private String errorMsg;

    /** 优先级 */
    private Integer priority;
}

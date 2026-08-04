package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 system_message_template 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_message_template")
public class SystemMessageTemplate extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 业务类型 */
    private String bizType;

    /** 渠道类型 */
    private Integer channelType;

    /** 消息标题 */
    private String title;

    /** 模板正文 */
    private String content;

    /** 变量定义 */
    private String variables;

    /** 渠道差异配置 */
    private String channelConfig;

    /** 降级渠道 */
    private Integer fallbackChannelType;

    /** 渠道编码 */
    private String channelCode;

    /** 状态 */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;
}

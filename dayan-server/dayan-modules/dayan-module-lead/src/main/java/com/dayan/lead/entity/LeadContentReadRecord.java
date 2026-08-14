package com.dayan.lead.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 lead_content_read_record 对应实体（内容阅读线索记录）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lead_content_read_record")
public class LeadContentReadRecord extends BaseEntity {

    /** 主键（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 线索编码（lead_info.lead_code） */
    private String leadCode;

    /** 访客令牌（冗余，便于直查） */
    private String visitorToken;

    /** 渠道编码（分享人所属渠道） */
    private String channelCode;

    /** 分享人代理人编码（NULL=直接访问） */
    private String agentCode;

    /** 内容编码 */
    private String contentCode;

    /** 内容标题（冗余快照） */
    private String contentTitle;
}

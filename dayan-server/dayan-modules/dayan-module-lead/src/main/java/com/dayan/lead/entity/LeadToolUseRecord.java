package com.dayan.lead.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 lead_tool_use_record 对应实体（工具使用线索记录）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lead_tool_use_record")
public class LeadToolUseRecord extends BaseEntity {

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

    /** 工具编码（tool_info.tool_code；历史数据可能为路径标识） */
    private String toolCode;

    /** 工具名称（冗余快照） */
    private String toolName;
}

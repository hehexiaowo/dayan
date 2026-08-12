package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 线索互动记录（agent_lead_trace）。
 *
 * <p>记录客户浏览分享内容、使用工具、查看海报等互动行为。
 * 每次打开分享链接产生一条记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_lead_trace")
public class AgentLeadTrace extends BaseEntity {

    /** 主键（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联 agent_lead.id */
    private Long leadId;

    /** 代理人编码 */
    private String agentCode;

    /** 渠道编码 */
    private String channelCode;

    /** 互动类型（1=浏览内容, 2=使用工具, 3=查看海报） */
    private Integer traceType;

    /** 业务编码（contentCode/toolKey/templateCode） */
    private String bizCode;

    /** 展示标题 */
    private String bizTitle;

    /** 互动时间 */
    private LocalDateTime traceTime;
}

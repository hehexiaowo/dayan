package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 agent_share_record 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_share_record")
public class AgentShareRecord extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 分享编码 */
    private String shareCode;

    /** 代理人编码 */
    private String agentCode;

    /** 分享类型 */
    private Integer shareType;

    /** 分享对象编码 */
    private String bizCode;

    /** 分享渠道 */
    private Integer shareChannel;

    /** 接收客户编码 */
    private String clientCode;

    /** 浏览次数 */
    private Integer viewCount;

    /** 分享时间 */
    private LocalDateTime shareTime;
}

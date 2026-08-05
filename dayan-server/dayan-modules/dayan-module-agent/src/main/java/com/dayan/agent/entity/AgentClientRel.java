package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 agent_client_rel 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_client_rel")
public class AgentClientRel extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 代理人编码 */
    private String agentCode;

    /** 客户编码 */
    private String clientCode;

    /** 绑定类型 */
    private Integer bindType;

    /** 绑定时间 */
    private LocalDateTime bindTime;

    /** 状态 */
    private Integer status;
}

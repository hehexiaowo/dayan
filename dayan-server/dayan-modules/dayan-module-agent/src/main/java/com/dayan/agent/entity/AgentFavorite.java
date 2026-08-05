package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 agent_favorite 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_favorite")
public class AgentFavorite extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 代理人编码 */
    private String agentCode;

    /** 收藏对象类型 */
    private Integer targetType;

    /** 收藏对象编码 */
    private String targetCode;
}

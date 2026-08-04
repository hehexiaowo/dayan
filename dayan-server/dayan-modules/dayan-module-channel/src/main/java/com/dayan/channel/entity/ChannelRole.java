package com.dayan.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 channel_role 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("channel_role")
public class ChannelRole extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 所属渠道编码 */
    private String channelCode;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 角色类型 */
    private Integer roleType;

    /** 角色描述 */
    private String description;

    /** 状态 */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;
}

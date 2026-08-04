package com.dayan.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 channel_role_permission_ship 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("channel_role_permission_ship")
public class ChannelRolePermissionShip extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 角色编码 */
    private String roleCode;

    /** 权限编码 */
    private String permissionCode;
}

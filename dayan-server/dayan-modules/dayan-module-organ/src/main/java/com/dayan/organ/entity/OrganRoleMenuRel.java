package com.dayan.organ.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 organ_role_menu_rel 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("organ_role_menu_rel")
public class OrganRoleMenuRel extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 角色编码 */
    private String roleCode;

    /** 菜单编码 */
    private String menuCode;

    /** 所属组织编码 */
    private String organCode;
}

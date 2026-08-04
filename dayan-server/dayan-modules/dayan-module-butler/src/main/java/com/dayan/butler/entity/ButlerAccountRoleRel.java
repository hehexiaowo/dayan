package com.dayan.butler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 butler_account_role_rel 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("butler_account_role_rel")
public class ButlerAccountRoleRel extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 管家账号编码 */
    private String accountCode;

    /** 管家编码 */
    private String butlerCode;

    /** 角色类型 */
    private Integer roleType;

    /** 角色描述 */
    private String description;
}

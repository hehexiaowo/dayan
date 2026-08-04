package com.dayan.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 supplier_account_role_rel 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier_account_role_rel")
public class SupplierAccountRoleRel extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 账号编码 */
    private String accountCode;

    /** 角色编码 */
    private String roleCode;

    /** 供应商编码 */
    private String supplierCode;
}

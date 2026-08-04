package com.dayan.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 supplier_contact 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier_contact")
public class SupplierContact extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 供应商编码 */
    private String supplierCode;

    /** 联系人姓名 */
    private String contactName;

    /** 联系人类型 */
    private Integer contactType;

    /** 职位 */
    private String position;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 微信号 */
    private String wechat;

    /** 是否主联系人 */
    private Integer isPrimary;

    /** 备注 */
    private String remark;
}

package com.dayan.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 supplier_account 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier_account")
public class SupplierAccount extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 供应商编码 */
    private String supplierCode;

    /** 账号编码 */
    private String accountCode;

    /** 登录用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 密码盐值 */
    private String salt;

    /** 真实姓名 */
    private String realName;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 微信OpenID */
    private String openId;

    /** 微信UnionID */
    private String unionId;

    /** 邮箱 */
    private String email;

    /** 职位 */
    private String position;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 累计登录次数 */
    private Integer loginCount;

    /** 密码修改时间 */
    private LocalDateTime pwdUpdateTime;

    /** 账号状态 */
    private Integer accountStatus;

    /** 是否管理员账号 */
    private Integer isAdmin;
}

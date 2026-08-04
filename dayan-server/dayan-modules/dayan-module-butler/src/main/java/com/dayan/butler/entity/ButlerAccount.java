package com.dayan.butler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 butler_account 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("butler_account")
public class ButlerAccount extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 管家编码 */
    private String butlerCode;

    /** 登录用户名 */
    private String username;

    /** 手机号 */
    private String phone;

    /** 密码 */
    private String password;

    /** 密码盐值 */
    private String salt;

    /** 微信OpenID */
    private String openId;

    /** 微信UnionID */
    private String unionId;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 账号状态 */
    private Integer accountStatus;
}

package com.dayan.client.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 client_account 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("client_account")
public class ClientAccount extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 客户编码 */
    private String clientCode;

    /** 所属渠道编码 */
    private String channelCode;

    /** 登录用户名 */
    private String username;

    /** 登录手机号 */
    private String phone;

    /** 密码 */
    private String password;

    /** 密码盐值 */
    private String salt;

    /** 微信OpenID */
    private String openId;

    /** 微信UnionID */
    private String unionId;

    /** 支付宝账号ID */
    private String alipayId;

    /** 渠道账号系统唯一编码 */
    private String extAccountNo;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 累计登录次数 */
    private Integer loginCount;

    /** 账号状态 */
    private Integer accountStatus;
}

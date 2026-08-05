package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 agent_account 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_account")
public class AgentAccount extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 代理人编码 */
    private String agentCode;

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

    /** 渠道账号系统唯一编码 */
    private String extAccountNo;

    /** 账号状态 */
    private Integer accountStatus;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;
}

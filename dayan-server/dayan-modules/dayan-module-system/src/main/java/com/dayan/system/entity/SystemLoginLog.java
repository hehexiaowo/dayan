package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 表 system_login_log 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_login_log")
public class SystemLoginLog extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 客户编码 */
    private String clientCode;

    /** 登录方式 */
    private Integer loginType;

    /** 登录手机号 */
    private String phone;

    /** 第三方OpenID */
    private String openId;

    /** 登录IP */
    private String loginIp;

    /** 登录地域 */
    private String loginLocation;

    /** 设备类型 */
    private Integer deviceType;

    /** 设备信息 */
    private String deviceInfo;

    /** 结果 */
    private Integer result;

    /** 失败原因 */
    private String failReason;

    /** 登录时间 */
    private LocalDateTime loginTime;
}

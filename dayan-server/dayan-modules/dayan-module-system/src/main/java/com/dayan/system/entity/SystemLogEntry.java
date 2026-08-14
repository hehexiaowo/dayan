package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.dayan.common.mybatis.entity.BaseEntity;
import com.dayan.system.enums.SystemLogSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统日志公共字段基类（system_log_organ / system_log_channel / system_log_agent / system_log_client 四表同构）。
 *
 * <p>日志按账号类型（Sa-Token loginType）分表路由，见 {@link SystemLogSource#fromAccountType}。
 * 登录/登出事件以 module='auth', action='login'/'logout' 入表，不再单设 login_log。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class SystemLogEntry extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 链路追踪ID */
    private String traceId;

    /** 账号类型（admin/channel/agent/client/supplier/distributor/system） */
    private String accountType;

    /** 操作账号编码 */
    private String accountCode;

    /** 操作人姓名 */
    private String accountName;

    /** 操作模块（auth=登录登出） */
    private String module;

    /** 操作动作（login/logout/create/update/delete等） */
    private String action;

    /** 操作描述 */
    private String actionDescription;

    /** 操作对象类型 */
    private String targetType;

    /** 操作对象编码 */
    private String targetCode;

    /** 操作对象描述 */
    private String targetDescription;

    /** 请求URL */
    private String requestUrl;

    /** 请求方法 */
    private String requestMethod;

    /** 请求参数（脱敏后） */
    private String requestParams;

    /** 响应结果（JSON，超长截断） */
    private String responseResult;

    /** 响应状态码 */
    private Integer responseCode;

    /** 操作IP地址 */
    private String ipAddress;

    /** IP归属地 */
    private String ipLocation;

    /** 浏览器UA */
    private String userAgent;

    /** 设备类型（pc/mobile/tablet） */
    private String deviceType;

    /** 操作系统 */
    private String os;

    /** 浏览器 */
    private String browser;

    /** 结果（0=失败, 1=成功） */
    private Integer resultStatus;

    /** 错误信息 */
    private String errorMsg;

    /** 执行耗时（毫秒） */
    private Integer duration;

    /** 该实体对应的日志来源（决定落哪张表） */
    public abstract SystemLogSource logSource();
}

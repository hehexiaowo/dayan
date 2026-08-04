package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 system_log_organ 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_log_organ")
public class SystemLogOrgan extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 组织编码 */
    private String organCode;

    /** 操作账号编码 */
    private String accountCode;

    /** 操作人姓名 */
    private String accountName;

    /** 操作模块 */
    private String module;

    /** 操作动作 */
    private String action;

    /** 操作对象类型 */
    private String targetType;

    /** 操作对象编码 */
    private String targetCode;

    /** 操作内容描述 */
    private String content;

    /** 变更前数据 */
    private String beforeData;

    /** 变更后数据 */
    private String afterData;

    /** 操作IP地址 */
    private String ipAddress;

    /** 浏览器UA */
    private String userAgent;

    /** 请求URL */
    private String requestUrl;

    /** 请求方法 */
    private String requestMethod;

    /** 结果状态 */
    private Integer resultStatus;

    /** 错误信息 */
    private String errorMsg;

    /** 执行耗时(毫秒) */
    private Integer duration;
}

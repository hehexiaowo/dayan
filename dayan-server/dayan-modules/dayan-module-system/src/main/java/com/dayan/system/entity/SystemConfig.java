package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 system_config 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SystemConfig extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 配置分组 */
    private String configGroup;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 值类型 */
    private String valueType;

    /** 环境 */
    private String env;

    /** 配置作用域 */
    private String scope;

    /** 组织编码 */
    private String organCode;

    /** 用户/账号编码 */
    private String userCode;

    /** 配置名称 */
    private String configName;

    /** 配置说明 */
    private String description;

    /** 是否敏感配置 */
    private Integer isSecret;

    /** 是否运行时热更新 */
    private Integer isRuntime;

    /** 排序号 */
    private Integer sortOrder;
}

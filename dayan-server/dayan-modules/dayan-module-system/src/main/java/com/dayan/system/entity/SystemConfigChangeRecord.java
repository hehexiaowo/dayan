package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 system_config_change_record 对应实体。
 *
 * <p>系统配置变更记录（原 system_config_log，38 号迁移起更名）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config_change_record")
public class SystemConfigChangeRecord extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 关联system_config.id */
    private Long configId;

    /** 配置分组 */
    private String configGroup;

    /** 配置键 */
    private String configKey;

    /** 环境 */
    private String env;

    /** 变更前值 */
    private String oldValue;

    /** 变更后值 */
    private String newValue;

    /** 操作类型 */
    private String action;

    /** 操作账号类型 */
    private String accountType;

    /** 操作账号编码 */
    private String accountCode;

}

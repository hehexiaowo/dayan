package com.dayan.tool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 tool_info 对应实体（工具实例定义，平台共享表）。
 *
 * <p>仅承载定义与配置：展示/路由细节（图标、颜色、页面路径、可见端、排序）
 * 由端上按 tool_type 固定映射，类型化配置走 config_json。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tool_info")
public class ToolInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工具编码（TL+5位序列） */
    private String toolCode;

    /** 工具名称 */
    private String toolName;

    /** 工具类型（pension/gap/aiartist/aichat） */
    private String toolType;

    /** 工具简介 */
    private String toolDesc;

    /** 工具配置 JSON（按类型承载提示词/默认值等） */
    private String configJson;

    /** 状态（0=禁用, 1=启用） */
    private Integer status;

    /** 备注 */
    private String remark;
}

package com.dayan.scene.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 scene_resource 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scene_resource")
public class SceneResource extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 场景编码 */
    private String sceneCode;

    /** 资源类型 */
    private Integer resourceType;

    /** 资源名称 */
    private String resourceName;

    /** 资源描述 */
    private String resourceDescription;

    /** 数量 */
    private Integer quantity;

    /** 单位 */
    private String unit;

    /** 单位成本 */
    private BigDecimal unitCost;

    /** 是否由机构提供 */
    private Integer isProvided;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}

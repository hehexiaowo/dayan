package com.dayan.scene.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 scene_item 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scene_item")
public class SceneItem extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 场景编码 */
    private String sceneCode;

    /** 项目编码 */
    private String itemCode;

    /** 项目名称 */
    private String itemName;

    /** 项目类型 */
    private Integer itemType;

    /** 项目描述 */
    private String itemDescription;

    /** 预计时长(分钟) */
    private Integer durationMinutes;

    /** 排序号 */
    private Integer sortOrder;

    /** 是否必选参与 */
    private Integer isRequired;

    /** 状态 */
    private Integer status;
}

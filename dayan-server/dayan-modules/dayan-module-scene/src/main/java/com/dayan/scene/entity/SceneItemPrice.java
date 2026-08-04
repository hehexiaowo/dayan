package com.dayan.scene.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 scene_item_price 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("scene_item_price")
public class SceneItemPrice extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 场景编码 */
    private String sceneCode;

    /** 场景项目编码 */
    private String sceneItemCode;

    /** 定价类型 */
    private Integer priceType;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 售价 */
    private BigDecimal salePrice;

    /** 渠道专属价 */
    private BigDecimal channelPrice;

    /** 价格说明 */
    private String priceDescription;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期 */
    private LocalDate expireDate;

    /** 状态 */
    private Integer status;
}

package com.dayan.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权益商品配置实体（1:1 关联 goods_info，商品类型特有配置）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods_equity")
public class GoodsEquity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商品编码（1:1关联goods_info.goods_code） */
    private String goodsCode;

    /** 使用人人数（1=个人版,2=双人版,3+家庭版） */
    private Integer personCount;

    /** 激活后有效天数 */
    private Integer validDays;

    /** 库存有效期天数（未激活时） */
    private Integer shelfLifeDays;

    /** 是否可转让（0否1是） */
    private Integer maxTransferable;

    /** 权益配置说明 */
    private String description;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=停用,1=启用） */
    private Integer status;
}

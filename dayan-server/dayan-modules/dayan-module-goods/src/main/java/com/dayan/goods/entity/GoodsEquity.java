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

    /** 使用人人数（1=个人版,2=双人版,3+家庭版；= holderRule 构成之和） */
    private Integer personCount;

    /** 权益期限类型（1=固定天数（validDays生效），2=终身） */
    private Integer validityType;

    /** 权益人构成规则JSON（HolderRule：self/spouse/parent/designateAtActivation） */
    private String holderRule;

    /** 配额归属（0=按人独立配额，1=权益人共享池） */
    private Integer shareMode;

    /** 激活后有效天数 */
    private Integer validDays;

    /** 库存有效期天数（未激活时） */
    private Integer shelfLifeDays;

    /** 可转让次数（0=不可转让，1/2/3=可转让N次） */
    private Integer maxTransferable;

    /** 权益配置说明 */
    private String description;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=停用,1=启用） */
    private Integer status;
}

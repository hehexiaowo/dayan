package com.dayan.equity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 equity_template 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("equity_template")
public class EquityTemplate extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 权益类型 */
    private Integer equityType;

    /** 权益等级 */
    private Integer equityLevel;

    /** 权益面值 */
    private BigDecimal equityValue;

    /** 成本价 */
    private BigDecimal costPrice;

    /** 权益内容描述 */
    private String contentDescription;

    /** 包含服务项目 */
    private String serviceItems;

    /** 适用机构范围 */
    private String applicableParks;

    /** 适用城市范围 */
    private String applicableCities;

    /** 激活后有效天数 */
    private Integer validDays;

    /** 库存有效期天数 */
    private Integer shelfLifeDays;

    /** 是否可转让 */
    private Integer isTransferable;

    /** 是否可叠加使用 */
    private Integer isStackable;

    /** 最大使用次数 */
    private Integer maxUseCount;

    /** 权益封面图 */
    private String coverImage;

    /** 卡面设计图URL */
    private String cardDesignUrl;

    /** 使用说明/条款 */
    private String terms;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}

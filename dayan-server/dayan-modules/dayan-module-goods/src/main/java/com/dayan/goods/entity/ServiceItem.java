package com.dayan.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 服务项目实体（原子服务能力：安排权益/费用权益两大类）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_item")
public class ServiceItem extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 服务项目编码（SI+5位） */
    private String itemCode;

    /** 服务项目名称 */
    private String itemName;

    /** 项目大类（1=安排权益，2=费用权益） */
    private Integer itemCategory;

    /** 安排权益子类（1=旅居,2=活力长居,3=照护长居）；费用权益为NULL */
    private Integer itemSubtype;

    /** 面值/单价（元） */
    private BigDecimal itemValue;

    /** 费用承担方（0=客户自负，1=系统承担） */
    private Integer costBearing;

    /** 服务网络JSON数组：元素为park_code或通配模式 */
    private String serviceNetwork;

    /** 费用权益补贴明细JSON：[{room_type,service_content,quantity}] */
    private String coveredItems;

    /** 激活后有效天数 */
    private Integer validDays;

    /** 最大使用次数 */
    private Integer maxUseCount;

    /** 服务项目说明 */
    private String description;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=停用,1=启用） */
    private Integer status;
}

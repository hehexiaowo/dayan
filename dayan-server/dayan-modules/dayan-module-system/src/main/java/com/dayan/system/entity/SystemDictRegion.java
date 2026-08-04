package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 system_dict_region 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_dict_region")
public class SystemDictRegion extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 行政区划代码 */
    private String regionCode;

    /** 区划名称 */
    private String regionName;

    /** 父级区划代码 */
    private String parentCode;

    /** 层级 */
    private Integer level;

    /** 拼音 */
    private String pinyin;

    /** 首字母 */
    private String firstLetter;

    /** 经度 */
    private BigDecimal lng;

    /** 纬度 */
    private BigDecimal lat;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}

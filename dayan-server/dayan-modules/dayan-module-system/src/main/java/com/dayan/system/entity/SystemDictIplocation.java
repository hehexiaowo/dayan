package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 system_dict_iplocation 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_dict_iplocation")
public class SystemDictIplocation extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** IP起始地址 */
    private String ipStart;

    /** IP结束地址 */
    private String ipEnd;

    /** IP起始数值 */
    private Long ipStartNum;

    /** IP结束数值 */
    private Long ipEndNum;

    /** 国家 */
    private String country;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 运营商 */
    private String isp;

    /** 关联区划代码 */
    private String regionCode;
}

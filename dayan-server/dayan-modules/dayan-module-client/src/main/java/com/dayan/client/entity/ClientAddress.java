package com.dayan.client.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 client_address 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("client_address")
public class ClientAddress extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 客户编码 */
    private String clientCode;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 省编码 */
    private String provinceCode;

    /** 城市编码 */
    private String cityCode;

    /** 区编码 */
    private String districtCode;

    /** 详细地址 */
    private String detailAddress;

    /** 完整地址 */
    private String fullAddress;

    /** 是否默认地址 */
    private Integer isDefault;

    /** 地址标签 */
    private String tag;
}

package com.dayan.client.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 client_favorite 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("client_favorite")
public class ClientFavorite extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 客户编码 */
    private String clientCode;

    /** 收藏对象类型 */
    private Integer targetType;

    /** 收藏对象编码 */
    private String targetCode;

    /** 收藏对象名称 */
    private String targetName;

    /** 备注 */
    private String remark;
}

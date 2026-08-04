package com.dayan.organ.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 organ_permission 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("organ_permission")
public class OrganPermission extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 权限编码 */
    private String permissionCode;

    /** 权限名称 */
    private String permissionName;

    /** 父权限编码 */
    private String parentCode;

    /** 权限类型 */
    private Integer permissionType;

    /** 路由/接口路径 */
    private String path;

    /** 请求方法 */
    private String method;

    /** 图标 */
    private String icon;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}

package com.dayan.client.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 client_family_member 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("client_family_member")
public class ClientFamilyMember extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 客户编码 */
    private String clientCode;

    /** 成员姓名 */
    private String memberName;

    /** 与客户关系 */
    private String relation;

    /** 性别 */
    private Integer gender;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 是否紧急联系人 */
    private Integer isEmergencyContact;

    /** 是否主要联系人 */
    private Integer isPrimaryContact;

    /** 是否决策人 */
    private Integer isDecisionMaker;

    /** 地址 */
    private String address;

    /** 备注 */
    private String remark;

    /** 状态 */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;
}

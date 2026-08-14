package com.dayan.butler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 butler_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("butler_info")
public class ButlerInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 管家编码 */
    private String butlerCode;

    /** 管家姓名 */
    private String fullName;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 所属组织编码 */
    private String organCode;

    /** 关联后台账号编码（organ_account.account_code，未开通为 NULL） */
    private String accountCode;

    /** 管家等级 */
    private Integer butlerLevel;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}

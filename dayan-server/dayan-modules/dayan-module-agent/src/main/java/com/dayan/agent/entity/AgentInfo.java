package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 agent_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_info")
public class AgentInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 代理人编码 */
    private String agentCode;

    /** 代理人姓名 */
    private String fullName;

    /** 性别 */
    private Integer gender;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 身份证号 */
    private String idCard;

    /** 所属渠道编码 */
    private String channelCode;

    /** 保险公司名称 */
    private String companyName;

    /** 分支机构 */
    private String branchName;

    /** 部门 */
    private String department;

    /** 职位 */
    private String position;

    /** 保险公司工号 */
    private String employeeNo;

    /** 从业资格证号 */
    private String licenseNo;

    /** 省份编码 */
    private String provinceCode;

    /** 城市编码 */
    private String cityCode;

    /** 区划编码 */
    private String districtCode;

    /** 详细地址 */
    private String address;

    /** 服务介绍 */
    private String serviceIntro;

    /** 服务客户数 */
    private Integer clientCount;

    /** 累计订单数 */
    private Integer totalOrderCount;

    /** 累计订单金额 */
    private BigDecimal totalOrderAmount;

    /** 等级 */
    private Integer agentLevel;

    /** 是否认证 */
    private Integer isCertified;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}

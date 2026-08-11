package com.dayan.agent.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 端个人资料聚合视图（agent_account + agent_info + 渠道/区划名称）。
 *
 * <p>phone 返回完整值，脱敏由前端展示层负责。
 */
@Data
@Builder
public class AgentProfileVO {

    /** 代理人编码 */
    private String agentCode;
    /** 姓名（agent_info.full_name） */
    private String fullName;
    /** 性别：0 保密 / 1 男 / 2 女 */
    private Integer gender;
    /** 头像（OSS key） */
    private String avatar;
    /** 手机号（完整值，前端脱敏） */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 登录用户名（agent_account.username，只读） */
    private String username;
    /** 渠道编码 */
    private String channelCode;
    /** 渠道简称 */
    private String channelName;
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
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String provinceName;
    private String cityName;
    private String districtName;
    /** 详细地址 */
    private String address;
    /** 服务介绍 */
    private String serviceIntro;
    /** 等级：1 普通 / 2 银牌 / 3 金牌 / 4 钻石 */
    private Integer agentLevel;
    /** 是否认证：0 否 / 1 是 */
    private Integer isCertified;
    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;
}

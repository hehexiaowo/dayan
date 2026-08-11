package com.dayan.agent.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Agent 端基础资料更新请求（白名单字段，其余字段后端一律忽略）。
 */
@Data
public class AgentProfileUpdateDTO {

    /** 姓名（已认证代理人不可修改，后端拦截） */
    @Size(min = 2, max = 20, message = "姓名长度须为 2-20 字")
    private String fullName;

    /** 性别：0 保密 / 1 男 / 2 女 */
    @Min(0) @Max(2)
    private Integer gender;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String email;

    /** 头像 OSS key（经 /v1/files/upload 上传后取得） */
    @Size(max = 500)
    private String avatar;

    private String provinceCode;
    private String cityCode;
    private String districtCode;

    @Size(max = 200)
    private String address;

    @Size(max = 200, message = "服务简介最多 200 字")
    private String serviceIntro;
}

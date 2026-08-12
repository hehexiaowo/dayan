package com.dayan.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Client 端个人资料自助更新请求（白名单字段，其余后端一律忽略）。
 *
 * <p>手机号 / 身份证 / 等级 / VIP / 统计字段均不可自助修改。
 */
@Data
public class ClientProfileUpdateDTO {

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

    private LocalDate birthday;

    private String provinceCode;
    private String cityCode;
    private String districtCode;

    @Size(max = 200)
    private String address;
}

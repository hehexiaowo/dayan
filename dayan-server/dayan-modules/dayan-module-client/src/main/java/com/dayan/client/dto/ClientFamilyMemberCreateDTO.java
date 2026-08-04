package com.dayan.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客户家庭成员创建入参。
 */
@Data
public class ClientFamilyMemberCreateDTO {

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 64)
    private String clientCode;

    @NotBlank(message = "成员姓名不能为空")
    @Size(max = 64)
    private String memberName;

    @NotBlank(message = "与客户关系不能为空")
    @Size(max = 32)
    private String relation;

    private Integer gender;

    @Size(max = 32)
    private String phone;

    @Size(max = 128)
    private String email;

    private Integer isEmergencyContact;
    private Integer isPrimaryContact;
    private Integer isDecisionMaker;
    private String address;
    private String remark;
    private Integer status;
    private Integer sortOrder;
}

package com.dayan.client.dto;

import lombok.Data;

/**
 * 客户家庭成员更新入参。
 */
@Data
public class ClientFamilyMemberUpdateDTO {

    private String memberName;
    private String relation;
    private Integer gender;
    private String phone;
    private String email;
    private Integer isEmergencyContact;
    private Integer isPrimaryContact;
    private Integer isDecisionMaker;
    private String address;
    private String remark;
    private Integer status;
    private Integer sortOrder;
}

package com.dayan.client.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户家庭成员 VO。
 */
@Data
public class ClientFamilyMemberVO {

    private Long id;
    private String clientCode;
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
    private LocalDateTime createdAt;
}

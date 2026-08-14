package com.dayan.organ.vo;

import lombok.Data;

/**
 * 组织精简 VO（下拉选择用，仅含编码与名称）。
 */
@Data
public class OrganInfoSimpleVO {

    /** 组织编码 */
    private String organCode;

    /** 组织全称 */
    private String fullName;

    /** 简称 */
    private String shortName;

    /** 组织类型：1运营方/2子公司/3分公司 */
    private Integer organType;

    /** 状态：1启用/0禁用 */
    private Integer status;
}
